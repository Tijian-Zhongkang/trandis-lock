package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.*;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.netty.LeaderNettyRemotingServer;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.response.*;
import com.xm.sanvanfo.scriptor.*;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class Leader extends AbstractRole {

    private final UpdateMasterInfoScript updateMasterInfoScript;
    private final RemoveLeaderScript removeLeaderScript;
    private final ExecutorService executorService;
    private final Map<String, FollowerInfo> followersMap = new ConcurrentHashMap<>();
    private final Map<IInterProgressParser.InterProgressType, IInterProgressParser> parserMap = new ConcurrentHashMap<>();
    private final HashedWheelTimer timer;
    private final RepairRetryManager repairRetryManager;
    private LeaderNettyRemotingServer remotingServer;
    private BootstrapServerInfo bootstrapServerInfo;
    private volatile int needReElection;

    public Leader(CoordinatorConfig config, String id, IRedisLocker locker,
                  NettyServerConfig serverConfig, Compressor compressor, Serializer serializer, IServerBootstrapPlugin[] plugins, ChannelHandler... channelHandlers) {
        super(config, id, locker, RoleType.Leader);
        try {
            updateMasterInfoScript = new UpdateMasterInfoScript();
            removeLeaderScript = new RemoveLeaderScript();
            timer = new HashedWheelTimer();
            executorService = Executors.newFixedThreadPool(config.getDeleteFollowerThread(), new DefaultThreadFactory(config.getDeleteFollowerPrefix()));
            if (!config.getUseExistsNettyServer()) {
                IMessageTimeOutPlugin plugin = PluginLoader.INSTANCE().load(IMessageTimeOutPlugin.class, "redisCoordinatorServerTimeout");
                remotingServer = LeaderNettyRemotingServer.builder().config(serverConfig).plugins(plugins).handlers(channelHandlers)
                        .registerCompressors(compressor)
                        .registerSerializers(serializer)
                        .timeOutPlugins(new IMessageTimeOutPlugin[]{plugin})
                        .build();
                lockCoordinator.setServerInfo(remotingServer, remotingServer.getServerId());
            }
            repairRetryManager = new RepairRetryManager(id, config);
            parserMap.put(IInterProgressParser.InterProgressType.Lock, new LockParser(this));
            postConstruct();
            startAliveCheck();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public RoleType checkChange() {
        return 1 == needReElection ? RoleType.Candidate : RoleType.Leader;
    }

    @Override
    public boolean init() {
        return null != bootstrapServerInfo && StringUtils.isNotEmpty(bootstrapServerInfo.getIp());
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if(null != remotingServer) {
            remotingServer.dispose();
        }
        timer.stop();
        for (IInterProgressParser parser:parserMap.values()
             ) {
            parser.shutdown();
        }
        repairRetryManager.shutdown();
        List<CoordinatorMessage> messages = repairRetryManager.getSelfRepairCoordinatorMessages();
        for (CoordinatorMessage message:messages
             ) {
            lockCoordinator.pushMessage(message);
        }
    }

    @Override
    public void close() {
        Long result;
        for (IInterProgressParser parser:parserMap.values()
             ) {
            result = parser.close();
            if(!result.equals(1L)) {
                log.warn("close error parser type {}", parser.getClass().getCanonicalName());
            }
        }
        try {
            locker.deleteNode(coordinatorConfig.getNodeListKey(), id);
            result = locker.execScript(removeLeaderScript.script(), Long.class, null,
                    removeLeaderScript.scriptKey(coordinatorConfig.getLeaderKey()),
                    removeLeaderScript.scriptArgv(CoordinatorConst.idStr, id));
            log.warn("remove leader result is {}", result);
        }
        catch (Exception ex) {
            log.warn("close leader error ex is {}", BusinessException.exceptionFullMessage(ex));
        }

        super.close();
    }

    @Override
    void aliveCheck() throws Exception {
        Long now = Clock.systemDefaultZone().millis();
        bootstrapServerInfo = Optional.ofNullable(lockCoordinator.getNodeInfo()).map(NodeInfo::getBootstrapServerInfo).orElse(null);
        if(bootstrapServerInfo != null) {
            bootstrapServerInfo.setId(id);
            bootstrapServerInfo.setActiveTime(now);
        }
        needReElection = locker.execScript(updateMasterInfoScript.script(), Integer.class,null,
                updateMasterInfoScript.scriptKey(coordinatorConfig.getLeaderKey()),
                updateMasterInfoScript.scriptArgv(CoordinatorConst.idStr, id, CoordinatorConst.contentStr,
                        locker.getSerializer().serializeString(bootstrapServerInfo),
                        CoordinatorConst.activeTimeStr, now,  coordinatorConfig.getLeaderKeyTimeout()));

        log.info("{} is leader, info:{}, needRelection is {}", id, Optional.ofNullable(bootstrapServerInfo).map(BootstrapServerInfo::toString).orElse("null"), needReElection);
    }

    public void updateFollower(String id, ChannelHandlerContext ctx) {
        AbstractNettyRemoting remoting = lockCoordinator.getNodeInfo().getRemoting();
        Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
        AbstractNettyRemotingServer server = (AbstractNettyRemotingServer)remoting;
        NettyChannel channel = server.convertTo(ctx.channel());
        FollowerInfo newInfo = new FollowerInfo(channel,
                Clock.systemDefaultZone().millis(),
                new ArrayList<>(), new ArrayList<>(), new ReentrantLock(), FollowerInfo.InitStatus.NoInit, null);
        FollowerInfo preInfo = followersMap.putIfAbsent(id, newInfo);
        if(null != preInfo && preInfo.getInit().equals(FollowerInfo.InitStatus.Initing)) {
            return;
        }
        FollowerInfo info = followersMap.get(id);
        List<PluginRequest> requestList = new ArrayList<>();
        List<ResponseMessage> responseMessages = new ArrayList<>();
        info.getLock().lock();
        try {

            if (null == preInfo) {
                info.setInit(FollowerInfo.InitStatus.Initing);
                info.setChannel(channel);
                if (reBuildLockBusMessagesFail(ctx, id)) {
                    followersMap.remove(id);
                    info.setInit(FollowerInfo.InitStatus.NoInit);
                    return;
                }

                timer.newTimeout(new FollowerTimerTask(id), coordinatorConfig.getClientTimeoutMills(), TimeUnit.MILLISECONDS);
                log.info("add new follower node and time out {}", id);
                locker.addNode(coordinatorConfig.getNodeListKey(), id);
            }

            if (!followersMap.containsKey(id)) {
                log.debug("in lock get follower null:{}", id);
                return;
            }
            if (null != preInfo) {
                preInfo.setChannel(channel);
                if (preInfo.getInit().equals(FollowerInfo.InitStatus.NoInit)) {
                    info.setInit(FollowerInfo.InitStatus.Initing);
                    if (reBuildLockBusMessagesFail(ctx, id)) {
                        info.setInit(FollowerInfo.InitStatus.NoInit);
                        return;
                    }
                }
                preInfo.setUpdateTime(Clock.systemDefaultZone().millis());

            }
            if (info.getFailMessages().size() > 0) {
                log.debug("follower id is {} send fail response size is {}", id, info.getFailMessages().size());
                responseMessages.addAll(info.getFailMessages());
                info.getFailMessages().clear();
            }
            if (info.getFailRequests().size() > 0) {
                log.debug("follower id is {} send fail request size is {}", id, info.getFailRequests().size());
                requestList.addAll(info.getFailRequests());
                info.getFailRequests().clear();
            }
        }
        finally {
            info.getLock().unlock();
        }
        //copy the error messages because these maybe fail to add again, this measure prevents unlimited retransmission
        for (ResponseMessage response:responseMessages
             ) {
            log.debug("send fail response follower id is {}, data is {}", id, response.getResponse().toString());
           asyncSendResponse(server, response.getRpcRequestMessage(), response.getResponse(), channel, id);
        }

        for (PluginRequest request : requestList) {
            log.debug("send fail request follower id is {}, data is {}", id, request.toString());
            asyncSendRequest(server, request, channel, id);
        }
    }

    public void sendRequestTimeout(MessageFuture future) {
        sendRequestTimeout(future, (obj, request)-> {
            CoordinatorMessageWare messageWare = (CoordinatorMessageWare) obj[0];
            if (messageWare.timeoutResend()) {
                String destId = messageWare.getDestCoordinatorId();
                FollowerInfo followerInfo = followersMap.get(destId);
                if (null != followerInfo && null != followerInfo.getChannel()) {
                    log.warn("message time out resend fail {}", request.toString());
                    sendRequestFail(destId, followerInfo.getChannel(), request);
                } else {
                    log.warn("follower {} delete by leader", destId);
                }
            }
        });
    }

    public LockParser getLockParser() {
        return (LockParser)parserMap.get(IInterProgressParser.InterProgressType.Lock);
    }

    @Override
    boolean roleIsChanged() {
        return needReElection == 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    void dropFuture(CoordinatorMessage message) {
        Object body = message.getMessage().getBody();
        Asserts.isTrue(body instanceof PluginRequest);
        PluginRequest request = (PluginRequest)body;
        Object pluginBody = request.getBodyObj();
        if(null != pluginBody && CoordinatorMessageWare.class.isAssignableFrom(pluginBody.getClass())) {
            CoordinatorMessageWare coordinatorMessageWare = (CoordinatorMessageWare)pluginBody;
            if(coordinatorMessageWare.getCoordinatorId().equals(id)) {
                lockCoordinator.dropFuture(coordinatorMessageWare);
            }
            else {
                PluginResponse response = null;
                Class clazz = pluginBody.getClass();
                try {
                    response = get(clazz).defaultErrorResponse((CoordinatorMessageWare) pluginBody, "drop message " + pluginBody.toString());
                }
                catch (Exception ex) {
                    log.warn("message is error {}", BusinessException.exceptionFullMessage(ex));
                }

                if(null != response) {
                    FollowerInfo info = followersMap.get(coordinatorMessageWare.getCoordinatorId());
                    if(null == info) {
                        log.warn("follower delete by leader {}", coordinatorMessageWare.getCoordinatorId());
                        return;
                    }
                    NettyChannel channel = info.getChannel();
                    AbstractNettyRemoting remoting = lockCoordinator.getNodeInfo().getRemoting();
                    Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
                    AbstractNettyRemotingServer server = (AbstractNettyRemotingServer) remoting;
                    asyncSendResponse(server, message.getMessage(), response, channel, coordinatorMessageWare.getCoordinatorId());
                }
            }
        }

    }

    boolean isNotInit(String coordinatorId) {
        if(coordinatorId.equals(id)) {
            return false;
        }
        if(!followersMap.containsKey(coordinatorId)) {
            return true;
        }
        return !followersMap.get(coordinatorId).getInit().equals(FollowerInfo.InitStatus.Inited);
    }

    Map<String, FollowerInfo> getFollowersMap() {
        return Collections.unmodifiableMap(followersMap);
    }

    LockRepairManager getLockRepairManager() {
        LockParser parser = (LockParser)parserMap.get(IInterProgressParser.InterProgressType.Lock);
        return parser.getLockRepairManager();
    }

    RepairRetryManager getRepairRetryManager() {
        return repairRetryManager;
    }

    LockCoordinator getCoordinator() {return lockCoordinator;}

    IRedisLocker getLocker() {return locker;}

    void setNeedReElectionTrue() {
        this.needReElection = 1;
    }

    void asyncSendRequest(AbstractNettyRemotingServer server, PluginRequest request, NettyChannel channel, String id) {
        try {
            server.sendAsyncRequest(channel, request, (u, v) -> sendRequestFail(id, u == null ? channel : u, request));
        }
        catch (BusinessException ex) {
            sendRequestFail(id, channel, request);
        }
    }

    void asyncSendResponse(AbstractNettyRemotingServer server, RpcMessage rpcMessage, PluginResponse response, NettyChannel channel, String id) {
        if(null == channel) {
            sendResponseFail(null, new NettyResponse(rpcMessage, response), id);
            return;
        }
        try {
            server.sendAsyncResponse(rpcMessage, channel, response, (u, v) -> sendResponseFail(u, v, id));
        }
        catch (BusinessException ex) {
            sendResponseFail(channel, new NettyResponse(rpcMessage, response), id);
        }
    }

    int deleteFollower(String followerId, Boolean addRepair, ObjectHolder<Boolean> needRepair, boolean deleteWithoutTimeout) {
        try {
            Long now = Clock.systemDefaultZone().millis();
            FollowerInfo followerInfo = followersMap.get(followerId);
            if(null == followerInfo) {
                return 1;
            }
            followerInfo.getLock().lock();
            try {

                if(!followersMap.containsKey(followerId)) {
                    return 1;
                }
                if (deleteWithoutTimeout || null == followerInfo.getUpdateTime() || (now - followerInfo.getUpdateTime()) >= coordinatorConfig.getClientTimeoutMills()) {
                    log.debug("delete node by leader:{}, node id:{}", id, followerId);
                    for (IInterProgressParser parser:parserMap.values()
                         ) {
                        parser.deleteFollower(followerInfo, followerId, needRepair);
                        if(needRepair.getObj()) {
                           break;
                        }
                    }
                    if (!needRepair.getObj()) {
                        followerInfo.setInit(FollowerInfo.InitStatus.NoInit);
                        locker.deleteNode(coordinatorConfig.getNodeListKey(), followerId);
                        followersMap.remove(followerId);
                    }
                    if(addRepair && null != needRepair.getObj() && needRepair.getObj()) {
                        log.warn("add retry task deleteFollower data: {}", followerId);
                        repairRetryManager.addRetryTask(new Leader.DeleteFollowerData(followerId, id), ()->{
                            ObjectHolder<Boolean> objectHolder = new ObjectHolder<>();
                            deleteFollower(followerId, false, objectHolder, false);
                            return !objectHolder.getObj();
                        });
                    }
                    return 1;
                } else {
                    return 0;
                }
            }
            finally {
                followerInfo.getLock().unlock();
            }

        }
        catch (Exception ex) {
            log.error("delete node {} error:{}", followerId, BusinessException.exceptionFullMessage(ex));
            return 0;
        }
    }

    private void postConstruct() {
        if(null != remotingServer) {
            remotingServer.start();
        }
        List<String> nodes = locker.getAllNode(coordinatorConfig.getNodeListKey());

        for (String node:nodes
        ) {
            if(node.equals(id)) {
                continue;
            }
            followersMap.put(node, new FollowerInfo(null,
                    Clock.systemDefaultZone().millis(),
                    new ArrayList<>(), new ArrayList<>(), new ReentrantLock(), FollowerInfo.InitStatus.NoInit, null));
            for (IInterProgressParser parser:parserMap.values()
                 ) {
                parser.initFollowerNode(node);
            }
            timer.newTimeout(new FollowerTimerTask(node), coordinatorConfig.getClientTimeoutMills(), TimeUnit.MILLISECONDS);
            log.info("add new time out {}", node);
        }
        locker.addNode(coordinatorConfig.getNodeListKey(), id);
        for (IInterProgressParser parser:parserMap.values()
        ) {
            parser.initLeader();
        }

    }

    private void sendResponseFail(NettyChannel channel, NettyResponse response, String coordinatorId) {

        if(null == coordinatorId || null == response.getMsg() || !(BaseResponse.class.isAssignableFrom(((PluginResponse)(response.getMsg())).getBodyObj().getClass()))) {

            log.warn("coordinateId is null or response is not BaseResponse,coordinateId: {}, msg:{}",
                    coordinatorId, null == response.getMsg() ? "null" : response.getMsg().toString());
            return;
        }
        log.info("send response error channel: {}, coordinateId: {}, msg:{}", null == channel ? "null" : channel.getKey().toString(), coordinatorId, response.getMsg());
        if(!followersMap.containsKey(coordinatorId)) {
            log.warn("follower map does not contain id: {}", coordinatorId);
        }
        else {
            FollowerInfo followerInfo = followersMap.get(coordinatorId);
            followerInfo.getLock().lock();
            try {
                if(!followersMap.containsKey(coordinatorId)) {
                    return;
                }
                followerInfo.getFailMessages().add(new ResponseMessage(response.getRpcMessage(), (PluginResponse) response.getMsg()));
            }
            finally {
                followerInfo.getLock().unlock();
            }
        }
    }

    private void sendRequestFail(String coordinatorId, NettyChannel channel, Object request) {
        log.debug("send request error channel: {}  content: {}",
                null == channel || null == channel.getKey() ? "null channel" : channel.getKey().toString(), request.toString());
        Asserts.isTrue(request instanceof PluginRequest);
        PluginRequest pluginRequest = (PluginRequest)request;
        Asserts.isTrue(CoordinatorMessageWare.class.isAssignableFrom(pluginRequest.getBodyObj().getClass()));
        FollowerInfo followerInfo = followersMap.get(coordinatorId);
        if(null == followerInfo) {
            log.warn("follower {} is delete by leader", coordinatorId);
            return;
        }
        followerInfo.getLock().lock();
        try {
            if(!followersMap.containsKey(coordinatorId)) {
                log.warn("follower {} is delete by leader", coordinatorId);
                return;
            }
            followerInfo.getFailRequests().add(pluginRequest);
        }
        finally {
            followerInfo.getLock().unlock();
        }
    }


    private boolean reBuildLockBusMessagesFail(ChannelHandlerContext ctx, String id) {
        for (IInterProgressParser parser:parserMap.values()
             ) {
            if(parser.rebuildFollowerFail(ctx, id)) {
                return true;
            }
        }
        return false;
    }

    private void asyncDeleteFollower(Timeout timeout, String followerId) {
        ObjectHolder<Boolean> objectHolder = new ObjectHolder<>();
        Future<Integer> future = executorService.submit(()-> deleteFollower(followerId, true, objectHolder, false));
        try {
            Integer result = future.get();
            if(result.equals(0)) {
                timeout.timer().newTimeout(timeout.task(), coordinatorConfig.getClientTimeoutMills(), TimeUnit.MILLISECONDS);
                log.debug("renew new time out {}", followerId);
            }
        }
        catch (InterruptedException | ExecutionException ex) {
            log.error("future get error:{}", BusinessException.exceptionFullMessage(ex));
        }
    }

    private class FollowerTimerTask implements TimerTask {

        private String followerId;

        FollowerTimerTask(String followerId) {
            this.followerId = followerId;
        }

        @Override
        public void run(Timeout timeout) {
            asyncDeleteFollower(timeout, followerId);
        }
    }

    @AllArgsConstructor
    @Data
    private static class DeleteFollowerData {
        private String followerId;
        private String leaderId;
    }

}
