package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.*;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.RetryUtils;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.netty.FollowerNettyRemotingClient;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import com.xm.sanvanfo.protocol.request.*;
import com.xm.sanvanfo.scriptor.CheckLeaderScript;
import com.xm.sanvanfo.scriptor.ReElectionScript;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.netty.MessageFuture;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

@Slf4j
public class Follower extends AbstractRole implements IDefaultResponse  {

    @AllArgsConstructor
    @Data
    private static class NettyRequest {
        private CoordinatorMessage message;
        private Object obj;
    }

    private class SendRequestTimeoutPlugin implements IMessageTimeOutPlugin {

        @Override
        public void accept(MessageFuture future) {
            sendRequestTimeout(future);
        }
    }

    private volatile boolean needReElection;
    private Deque<NettyRequest> failDeque = new ConcurrentLinkedDeque<>();
    private final FollowerNettyRemotingClient followerNettyRemotingClient;
    private final CheckLeaderScript checkLeaderScript;
    private final ReElectionScript reElectionScript;
    private BootstrapServerInfo bootstrapServerInfo;
    public Follower(CoordinatorConfig config, NettyClientConfig nettyClientConfig,
                    String id, IRedisLocker locker, EventExecutorGroup eventExecutorGroup, Compressor compressor, Serializer serializer) {
        super(config, id, locker, RoleType.Follower);
        checkLeaderScript = new CheckLeaderScript();
        reElectionScript = new ReElectionScript();
        followerNettyRemotingClient = FollowerNettyRemotingClient.builder().config(nettyClientConfig)
                .coordinatorConfig(config).eventExecutorGroup(eventExecutorGroup)
                .registerCompressors(compressor)
                .registerSerializers(serializer)
                .messageTimeoutPlugins(new IMessageTimeOutPlugin[]{new SendRequestTimeoutPlugin()})
                .follower(this).build();
        lockCoordinator.setServerInfo(followerNettyRemotingClient, "Client:" + id);
        followerNettyRemotingClient.start();
        startAliveCheck();

    }

    @Override
    public RoleType checkChange() {
        return needReElection ? RoleType.Candidate : RoleType.Follower;
    }

    @Override
    public boolean init() throws Exception {

        boolean ret = false;
         if(null != bootstrapServerInfo) {
             String serverInfoId  = bootstrapServerInfo.getId();
             Long activeTime = bootstrapServerInfo.getActiveTime();
             try {
                 followerNettyRemotingClient.validateConnection(bootstrapServerInfo, coordinatorConfig.getRetryTimes(),
                         RetryUtils.RetryType.EXP, coordinatorConfig.getFailWaitMills(), ()->{
                            try {
                                aliveCheck();
                                return bootstrapServerInfo;
                            }
                            catch (Exception ex) {
                                return null;
                            }
                         }, () ->{
                         if(!failDeque.isEmpty()) {
                             NettyRequest o = failDeque.pollFirst();
                             while ( o != null) {
                                 Asserts.isTrue(o.obj instanceof PluginRequest);
                                 CoordinatorMessage message = o.getMessage();
                                 Object obj = o.getObj();
                                 Asserts.isTrue(CoordinatorThreadMessageWare.class.isAssignableFrom(((PluginRequest)obj).getObj().getClass()));
                                 CoordinatorThreadMessageWare messageWare = (CoordinatorThreadMessageWare)((PluginRequest)obj).getObj();
                                 if(messageWare.getDestCoordinatorId().equals(bootstrapServerInfo.getId())
                                        ) {
                                     log.debug("client send error request {}", o.obj.toString());
                                     followerNettyRemotingClient.sendAsyncRequest((PluginRequest) o.obj,
                                             (u, v) -> sendRequestFail(message, u, v.getRpcMessage().getBody(),
                                                     () ->get(messageWare.getClass()).defaultErrorResponse(messageWare,
                                                             "resend message error " + messageWare.toString())
                                             ));
                                 }
                                 o = failDeque.pollFirst();
                             }
                         }
                 });
                 ret = true;
             }
             catch (IllegalArgumentException e) {
                 needReElection = false;
             }
             catch (BusinessException ex) {
                 log.warn("follower get bootstrap server info error:" + BusinessException.exceptionFullMessage(ex));
                 needReElection = 1 == reElection(serverInfoId, activeTime);
                 if(!needReElection) {
                     //fast get new leader
                     aliveCheck();
                 }
             }
         }
         else {
             needReElection = true;
         }
         return ret;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        followerNettyRemotingClient.dispose();
        //wait messages add queue again
        List<RpcMessage> rpcMessages = followerNettyRemotingClient.getAllLockedMessages();
        for (RpcMessage message:rpcMessages
             ) {
            Object obj = message.getBody();
            Asserts.isTrue(obj instanceof PluginRequest);
            Object lock = ((PluginRequest) obj).getBodyObj();
            if(lock instanceof AcquireRequest || lock instanceof ReleaseRequest) {
                lockCoordinator.pushMessage(new CoordinatorMessage(message));
            }
        }
    }

    @Override
    public void directProcess(CoordinatorMessage rpcMessage, CoordinatorMessageWare body) {
        try {
            get(body.getClass()).process(rpcMessage, body);
        }
        catch (Exception ex) {
            log.error("direct process error {}, ex {}", body.toString(), BusinessException.exceptionFullMessage(ex));
        }
    }

    @Override
    public void close() {
        PluginRequest request = new PluginRequest();
        request.setPlugin(CoordinatorConst.closeRequestPlugin);
        CloseRequest closeRequest = new CloseRequest();
        closeRequest.setCoordinatorId(id);
        closeRequest.setAppName(coordinatorConfig.getAppName());
        closeRequest.setDestCoordinatorId(followerNettyRemotingClient.getBootstrapServerInfo().getId());
        request.setObj(closeRequest);
        try {
            followerNettyRemotingClient.sendAsyncRequest(request,
                    (o, v) -> log.error("send close error channel {}, message {}", null == o ? "" : o.toString(), null == v ? "" : v.toString()));
        }
        catch (BusinessException ex) {
            log.error("send close error, message is {}", BusinessException.exceptionFullMessage(ex));
        }
        super.close();
    }

    public PluginRequest createHeartBeat() {
        PluginRequest request = new PluginRequest();
        request.setPlugin(CoordinatorConst.heartBeatRequestPlugin);
        HeartBeatRequest heartBeat = new HeartBeatRequest();
        heartBeat.setCoordinatorId(id);
        heartBeat.setAppName(coordinatorConfig.getAppName());
        heartBeat.setDestCoordinatorId(followerNettyRemotingClient.getBootstrapServerInfo().getId());
        request.setObj(heartBeat);
        return request;
    }

    @Override
    void aliveCheck() throws Exception {
        long now = Clock.systemDefaultZone().millis();
        now = now - coordinatorConfig.getLeaderKeyTimeout();
        bootstrapServerInfo = locker.execScript(checkLeaderScript.script(), BootstrapServerInfo.class,null,
                checkLeaderScript.scriptKey(coordinatorConfig.getLeaderKey()),
                checkLeaderScript.scriptArgv(CoordinatorConst.contentStr, CoordinatorConst.activeTimeStr, now));
        log.info("{} is follower, leader info:{}", id, Optional.ofNullable(bootstrapServerInfo).map(BootstrapServerInfo::toString).orElse("null"));
    }


    @Override
    boolean checkSkip(Object bodyObj) {
        boolean ret = false;
        if(CoordinatorMessageWare.class.isAssignableFrom(bodyObj.getClass())) {
            CoordinatorMessageWare messageWare = (CoordinatorMessageWare)bodyObj;
            if(messageWare.toServer()) {
                ret = !messageWare.getCoordinatorId().equals(id);
            }
            else {
                ret = !messageWare.getDestCoordinatorId().equals(id);
            }
        }
        if(ret) {
            log.warn("role changes causing data from other nodes to be processed {}", bodyObj.toString());
        }
        return ret;
    }

    @Override
    boolean roleIsChanged() {
        return needReElection;
    }

    FollowerNettyRemotingClient getClient() {
        return followerNettyRemotingClient;
    }

    void sendRequestFail(CoordinatorMessage message, NettyChannel channel, Object request, Supplier<PluginResponse> responseSupplier) {
        log.info("send request error address is : {}", null ==  channel.getKey().toString());
        long now = Clock.systemDefaultZone().millis();
        if (now - message.getAddTime() > coordinatorConfig.getMaxProcessWaitTime()
                || (!coordinatorConfig.getDropForFailTimes().equals(-1) && message.getFailTimes() >= coordinatorConfig.getDropForFailTimes())) {
            log.warn("drop message, message is {} now:{}", message.toString(), now);
            lockCoordinator.processResponse(responseSupplier.get());

        } else {
            log.warn("send exception add fail deque message is {}", request.toString());
            message.setFailTimes(message.getFailTimes() + 1);
            failDeque.offerLast(new NettyRequest(message, request));
        }

    }

    private void sendRequestTimeout(MessageFuture future) {

        NettyChannel channel = followerNettyRemotingClient.getNettyChannel();
        sendRequestTimeout(future, (obj, request)-> {
            CoordinatorMessageWare messageWare = (CoordinatorMessageWare) obj[0];
            if (messageWare.timeoutResend()) {
                CoordinatorFuture future1 = lockCoordinator.getFuture(messageWare.getId());
                if (null != future1) {
                    sendRequestFail(future1.getMessage(), channel, obj[1], () -> get(messageWare.getClass()).defaultErrorResponse(messageWare,
                            "resend message timeout error " + messageWare.toString()));
                }
            }
        });
    }



    private int reElection(String masterId, Long activeTime) throws Exception {
        return locker.execScript(reElectionScript.script(), Integer.class, null, reElectionScript.scriptKey(coordinatorConfig.getElectionKey(),
                coordinatorConfig.getNodeListKey(), coordinatorConfig.getLeaderKey()),
                reElectionScript.scriptArgv(id, coordinatorConfig.getElectionTimeout(), CoordinatorConst.idStr, masterId,
                        CoordinatorConst.activeTimeStr, activeTime));
    }

}
