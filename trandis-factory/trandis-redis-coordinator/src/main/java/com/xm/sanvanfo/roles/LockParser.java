package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.GetLockBusRequest;
import com.xm.sanvanfo.protocol.request.NotifyRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.GetLockBusResponse;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.scriptor.CheckOneWaitPathScript;
import com.xm.sanvanfo.scriptor.DeleteFollowerScript;
import com.xm.sanvanfo.scriptor.LoadNodeLockInfoScript;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.transaction.TransactionException;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LockParser implements IInterProgressParser,LockPathSplitter {

    private final Leader leader;
    private final LoadNodeLockInfoScript loadNodeLockInfoScript;
    private final CheckOneWaitPathScript checkOneWaitPathScript;
    private final DeleteFollowerScript deleteFollowerScript;
    private final LockRepairManager lockRepairManager;

    LockParser(Leader leader) {
        this.leader = leader;
        loadNodeLockInfoScript = new LoadNodeLockInfoScript();
        checkOneWaitPathScript = new CheckOneWaitPathScript();
        deleteFollowerScript = new DeleteFollowerScript();
        lockRepairManager = new LockRepairManager(leader.getId(), leader.getConfig(), leader.getLocker(), leader);
    }

    @Override
    public void shutdown() {
        lockRepairManager.shutdown();
    }

    @Override
    public void initFollowerNode(String node) {
        rebuildLockRedis(node);
    }

    @Override
    public void initLeader() {
        lockRepairManager.reBuildLeaderLock(LockMessageBus.INSTANCE().getLockInfo());
        List<String> list = LockMessageBus.INSTANCE().getAllWaitPath();
        checkNotifyWaitLocks(list);
    }

    @Override
    public void deleteFollower(FollowerInfo followerInfo, String followerId, ObjectHolder<Boolean> needRepair) throws Exception {
        if(!lockRepairManager.isRepairing(followerInfo.getNodeLockInfo())) {
            CoordinatorConfig coordinatorConfig = leader.getConfig();
            List ret = leader.getLocker().execScript(deleteFollowerScript.script(), List.class, null,
                    deleteFollowerScript.scriptKey(coordinatorConfig.getLeaderKey(),
                            followerId + "-" + coordinatorConfig.getLockWaitSuffix(),
                            followerId + "-" + coordinatorConfig.getLockListSuffix()),
                    deleteFollowerScript.scriptArgv(CoordinatorConst.idStr, leader.getId(), coordinatorConfig.getReadLockSuffix(), coordinatorConfig.getWriteLockSuffix(),
                            coordinatorConfig.getLockWaitSuffix(), coordinatorConfig.getNotifyWaitSuffix(),  CoordinatorConst.readEnterStr, CoordinatorConst.writeEnterStr,
                            CoordinatorConst.idStr));
            if (null == ret) {
                log.error("delete follower error result is null follower id is " + followerId);
            } else {
                Asserts.isTrue(ret.size() > 1);
                Long result = Long.parseLong(ret.get(0).toString());
                if (result.equals(1L)) {
                    log.info("delete lock wait info: {}", ret.get(1).toString());
                    int notifySize = Integer.parseInt(ret.get(2).toString());
                    int waiSize = Integer.parseInt(ret.get(3).toString());
                    List notifiedList = ret.subList(5, 5 + notifySize);
                    List  waitList = ret.size() > 5 + notifySize ? ret.subList(5 + notifySize, 5 + notifySize + waiSize) : new ArrayList();
                    List  lockList = ret.size() > 5 + notifySize + waiSize ? ret.subList(5 + notifySize + waiSize, ret.size()) : new ArrayList();
                    log.debug("delete notify {}", notifiedList.toString());
                    notifyWaitKeys(lockRepairManager.notifyLock(filterNotInit(notifiedList)));
                    needRepair.setObj(lockRepairManager.deleteNode(followerId, lockList, waitList));
                    if(needRepair.getObj()){
                        followerInfo.setInit(FollowerInfo.InitStatus.NoInitRepairing);
                    }
                    log.warn("delete node success id: {}", followerId);
                } else {
                    if(result.equals(-1L)) {
                        leader.setNeedReElectionTrue();
                    }
                    log.error("delete error code: {}, info: {}, follower id is {}", ret.get(0).toString(), ret.get(1).toString(), followerId);
                }

            }
        }
        else {
            needRepair.setObj(true);
        }
    }

    @Override
    public Long close() {
        try {
            CoordinatorConfig coordinatorConfig = leader.getConfig();
            String id = leader.getId();
            List ret = leader.getLocker().execScript(deleteFollowerScript.script(), List.class, null,
                    deleteFollowerScript.scriptKey(coordinatorConfig.getLeaderKey(),
                            id + "-" + coordinatorConfig.getLockWaitSuffix(),
                            id + "-" + coordinatorConfig.getLockListSuffix()),
                    deleteFollowerScript.scriptArgv(CoordinatorConst.idStr, id, coordinatorConfig.getReadLockSuffix(), coordinatorConfig.getWriteLockSuffix(),
                            coordinatorConfig.getLockWaitSuffix(), coordinatorConfig.getNotifyWaitSuffix(), CoordinatorConst.readEnterStr, CoordinatorConst.writeEnterStr,
                            CoordinatorConst.idStr));
            if(null == ret) {
                log.error("delete leader lock error result is null follower id is " + id);
            }
            else {
                Asserts.isTrue(ret.size() > 1);
                return Long.parseLong(ret.get(0).toString());
            }

        }
        catch (Exception ex) {
            log.error("leader close error is {}", BusinessException.exceptionFullMessage(ex));
        }
        return 0L;
    }

    @Override
    public boolean rebuildFollowerFail(ChannelHandlerContext ctx, String id) {
        FollowerInfo info = leader.getFollowersMap().get(id);
        if(null == info) {
            return false;
        }
        if(!leader.getFollowersMap().containsKey(id)) {
            return false;
        }
        AbstractNettyRemoting remoting = leader.getCoordinator().getNodeInfo().getRemoting();
        Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
        AbstractNettyRemotingServer server = (AbstractNettyRemotingServer) remoting;
        NettyChannel channel = server.convertTo(ctx.channel());
        PluginRequest request = new PluginRequest();
        request.setPlugin(CoordinatorConst.getLockBusRequestPlugin);
        GetLockBusRequest lockBusRequest = new GetLockBusRequest();
        lockBusRequest.setCoordinatorId(leader.getId());
        lockBusRequest.setDestCoordinatorId(id);
        lockBusRequest.setAppName(leader.getConfig().getAppName());
        request.setObj(lockBusRequest);
        try {
            Object obj = server.sendSyncRequest(channel, request);
            if(null == obj) {
                log.warn(String.format("fail get follower lock bus, follower id is %s message:send timeout", id));
                return true;
            }
            Asserts.isTrue(obj instanceof PluginResponse);
            Object pluginObj = ((PluginResponse) obj).getObj();
            Asserts.isTrue(pluginObj instanceof GetLockBusResponse);
            GetLockBusResponse response = (GetLockBusResponse) pluginObj;
            if (!response.getCode().equals(BaseResponse.ResponseCode.SUCCESS.getCode())) {
                log.warn(String.format("fail get follower lock bus, follower id is %s message:%s", id, response.getMsg()));
                return true;
            }
            if(!lockRepairManager.reBuildLock(id, response.getNodeLockInfo())) {
                log.warn("fail repair rebuild lock {}", id);
                return true;
            }
            ObjectHolder<Boolean> init = new ObjectHolder<>(true);
            Optional.ofNullable(response.getAcquireRequests()).ifPresent(o -> {
                for (GetLockBusResponse.RpcAcquire acquireRequest : o
                ) {
                    log.debug("rebuild acquire {}", acquireRequest.toString());
                    if(!scriptAcquire(acquireRequest, server, channel, true)) {
                        init.setObj(false);
                    }
                }
            });

            Optional.ofNullable(response.getReleaseRequests()).ifPresent(o -> {
                for (GetLockBusResponse.RpcRelease releaseRequest : o
                ) {
                    log.debug("rebuild release {}", releaseRequest.toString());
                    if(!scriptRelease(releaseRequest, server, channel, true)) {
                        init.setObj(false);
                    }
                }
            });

            Optional.ofNullable(response.getWaitList()).ifPresent(this::checkNotifyWaitLocks);
            if(init.getObj()) {
                info.setInit(FollowerInfo.InitStatus.Inited);
                log.info("rebuild lock bus success follower id {}, response is {}", id, response.toString());
            }
            else {
                log.warn("rebuild lock bus repairing follower id {}, response is {}", id, response.toString());
                return true;
            }
        } catch (Exception ex) {
            log.warn(String.format("fail rebuild lock bus, follower id is %s, exception:%s", id,
                    BusinessException.exceptionFullMessage(ex)));
            return true;
        }
        return false;
    }

    public void sendNextNotifyRequest(NotifyRequest notifyRequest) {
        String path = String.format("%s-%s-%s-%s", leader.getLocker().quote(notifyRequest.getPath(), leader.getConfig().getSpace()),
                notifyRequest.getDestCoordinatorId(), notifyRequest.getNotifyThreadId(), notifyRequest.getRead() ? "read" : "write");
        sendNextNotifyRequest(path);
    }

    LockRepairManager getLockRepairManager() {return lockRepairManager;}

    void notifyWaitKeys(List notifiedList) {
        for (Object o:notifiedList
        ) {
            LockSplit lockPart = splitNotifyLockPart((String)o);
            if(StringUtils.isEmpty(lockPart.getPath())) {
                log.error("notify lock local path is error:{}", o);
                continue;
            }
            String coordinatorId = lockPart.getCoordinatorId();
            String thread = lockPart.getThreadId();
            if(coordinatorId.equals(leader.getId())) {
                boolean ret = LockMessageBus.INSTANCE().notifyAll(thread);
                if(!ret) {
                    sendNextNotifyRequest((String)o);
                }
            }
            else {
                //notify follower
                FollowerInfo followerInfo = leader.getFollowersMap().get(coordinatorId);
                if(null == followerInfo) {
                    log.warn("notify WaitKeys follower is delete by leader follower id is :{}", coordinatorId);
                    continue;
                }
                followerInfo.getLock().lock();
                try {

                    if(!leader.getFollowersMap().containsKey(coordinatorId)) {
                        log.warn("in lock notify WaitKeys follower is delete by leader follower id is :{}", coordinatorId);
                        continue;
                    }
                    NettyChannel nettyChannel = followerInfo.getChannel();
                    AbstractNettyRemoting remoting = leader.getCoordinator().getNodeInfo().getRemoting();
                    PluginRequest pluginRequest = new PluginRequest();
                    pluginRequest.setPlugin(CoordinatorConst.notifyRequestPlugin);
                    NotifyRequest notify = new NotifyRequest();
                    notify.setNotifyCoordinatorId(coordinatorId);
                    notify.setNotifyThreadId(thread);
                    notify.setPath(lockPart.getPath());
                    notify.setRead(lockPart.getReadWrite().equals("read"));
                    notify.setCoordinatorId(leader.getId());
                    notify.setDestCoordinatorId(coordinatorId);
                    pluginRequest.setObj(notify);
                    if(null != nettyChannel) {
                        Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
                        AbstractNettyRemotingServer server = (AbstractNettyRemotingServer) remoting;
                        leader.asyncSendRequest(server, pluginRequest, nettyChannel, coordinatorId);
                    }
                    else {
                        log.warn("netty channel is null or remoting server is null, data id is {}", pluginRequest.toString());
                        followerInfo.getFailRequests().add(pluginRequest);
                    }

                }
                finally {
                    followerInfo.getLock().unlock();
                }
            }
        }
    }

    List filterNotInit(List notifies) {
        List<Object> result = new ArrayList<>();
        for (Object notify:notifies
        ) {
            LockSplit parts = splitNotifyLockPart(notify.toString());
            String coordinatorId = parts.getCoordinatorId();
            if(coordinatorId.equals(leader.getId())) {
                result.add(notify);
            }
            else {
                FollowerInfo followerInfo = leader.getFollowersMap().get(coordinatorId);
                if(null != followerInfo && followerInfo.getInit().equals(FollowerInfo.InitStatus.Inited)) {
                    result.add(notify);
                }
            }
        }
        return result;
    }

    private boolean  scriptRelease(GetLockBusResponse.RpcRelease releaseRequest, AbstractNettyRemotingServer server, NettyChannel channel, boolean addRepair) {

        ReleaseRequest release = releaseRequest.getReleaseRequest();
        try {
            IRequestProcessor processor = leader.get(ReleaseRequest.class);
            Asserts.isTrue(processor instanceof LeaderReleaseRequestProcessor);
            ReleaseResponse msg = ((LeaderReleaseRequestProcessor)processor).processReleaseInner(release);
            if(!msg.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode()) && !msg.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                //because it is a check, it must return a success
                msg.setCode(BaseResponse.ResponseCode.SUCCESS.getCode());
                leader.asyncSendResponse(server, buildRpcMessageCommon(releaseRequest),
                        leader.buildPluginResponse(msg, CoordinatorConst.releaseResponsePlugin), channel, release.getCoordinatorId());
            }
            if(msg.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                if(addRepair) {
                    log.warn("add retry task scriptRelease data: {}", releaseRequest.toString());
                    leader.getRepairRetryManager().addThreadRetryTask(release.getThreadId(), releaseRequest, () -> scriptRelease(releaseRequest, server, channel, false));
                }
                return false;
            }
            return true;
        }
        catch (Exception ex) {
            log.error("scriptRelease error:{}, acquire:{}", TransactionException.exceptionFullMessage(ex), release.toString());
            return true;
        }
    }

    private boolean scriptAcquire(GetLockBusResponse.RpcAcquire acquireRequest, AbstractNettyRemotingServer server, NettyChannel channel, Boolean addRepair) {
        AcquireRequest acquire = acquireRequest.getAcquireRequest();
        try {
            IRequestProcessor processor = leader.get(AcquireRequest.class);
            Asserts.isTrue(processor instanceof LeaderAcquireRequestProcessor);
            AcquireResponse msg = ((LeaderAcquireRequestProcessor)processor).processAcquireInner(acquire);
            if(!msg.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode()) && !msg.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                leader.asyncSendResponse(server, buildRpcMessageCommon(acquireRequest),
                        leader.buildPluginResponse(msg, CoordinatorConst.acquireResponsePlugin), channel, acquire.getCoordinatorId());
            }
            if(msg.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                if(addRepair) {
                    log.warn("add retry task scriptAcquire data: {}", acquireRequest.toString());
                    leader.getRepairRetryManager().addThreadRetryTask(acquire.getThreadId(), acquireRequest, () -> scriptAcquire(acquireRequest, server, channel, false));
                }
                return false;
            }
            return true;
        }
        catch (Exception ex) {
            log.error("scriptAcquire error:{}, acquire:{}", TransactionException.exceptionFullMessage(ex), acquire.toString());
            return true;
        }
    }

    private RpcMessage buildRpcMessageCommon(GetLockBusResponse.RpcCommon rpc) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBodyType(rpc.getBodyType());
        rpcMessage.setCodec(rpc.getCodec());
        rpcMessage.setCompressor(rpc.getCompressor());
        rpcMessage.setId(rpc.getId());
        rpcMessage.setMessageType(rpc.getMessageType());
        return rpcMessage;
    }

    private void checkNotifyWaitLocks(List<String> list) {
        List<String> needNotify = new ArrayList<>();
        for (String path:list
        ) {
            LockSplit pathParts = splitNotifyLockPart(path);
            try {
                String checkPath = pathParts.getPath();
                if(lockRepairManager.isRepairing(checkPath)) {
                    continue;
                }
                CoordinatorConfig coordinatorConfig = leader.getConfig();
                List exists = leader.getLocker().execScript(checkOneWaitPathScript.script(), List.class, null,
                        checkOneWaitPathScript.scriptKey(coordinatorConfig.getLeaderKey(), checkPath),
                        checkOneWaitPathScript.scriptArgv("id", leader.getId(), pathParts.getCoordinatorId() + "-" + pathParts.getThreadId() + "-" + pathParts.getReadWrite(),
                                coordinatorConfig.getLockWaitSuffix(), coordinatorConfig.getNotifyWaitSuffix(), "check"));
                Long result = Long.parseLong(exists.get(0).toString());
                if(result.equals(-1L)) {
                    leader.setNeedReElectionTrue();
                    return;
                }
                if (!lockRepairManager.checkOneWait(path, result) && (result.equals(0L) || result.equals(2L))) {
                    log.debug("check notify wait lock add path {}", path);
                    needNotify.add(path);
                }
            }
            catch (Exception ex) {
                log.error("check notify wait locks exec script error:{}", BusinessException.exceptionFullMessage(ex));
            }
        }
        notifyWaitKeys(lockRepairManager.notifyLock(needNotify));
    }

    private void sendNextNotifyRequest(String path) {
        try {
            List list = getNextNotified(path);
            notifyWaitKeys(lockRepairManager.notifyLock(filterNotInit(list)));
        }
        catch (Exception ex) {
            log.error("send request fail notify request get next wait key error:{}", BusinessException.exceptionFullMessage(ex));
        }
    }

    private List getNextNotified(String path) throws Exception {
        LockSplit pathParts = splitNotifyLockPart(path);
        String checkPath = pathParts.getPath();
        if(lockRepairManager.isRepairing(checkPath)) {
            return new ArrayList();
        }
        CoordinatorConfig coordinatorConfig = leader.getConfig();
        List list = leader.getLocker().execScript(checkOneWaitPathScript.script(), List.class, null,
                checkOneWaitPathScript.scriptKey(coordinatorConfig.getLeaderKey(), checkPath),
                checkOneWaitPathScript.scriptArgv("id", leader.getId(), pathParts.getCoordinatorId() + "-" + pathParts.getThreadId()+ "-" + pathParts.getReadWrite(),
                        coordinatorConfig.getLockWaitSuffix(), coordinatorConfig.getNotifyWaitSuffix(),  "get"));
        Long result = Long.parseLong(list.get(0).toString());
        log.debug("get next notify origin path:{}, result:{}", path, String.join(",", convertToString(list)));
        if(result.equals(0L)) {
            List next =  list.subList(2, list.size());
            if(!lockRepairManager.getNextNotified(path, next)) {
                return next;
            }
            else {
                log.debug("{} need repair", path);
            }
        }
        return new ArrayList();
    }

    private void rebuildLockRedis(String followerId) {
        try {
            CoordinatorConfig coordinatorConfig = leader.getConfig();
            List list = leader.getLocker().execScript(loadNodeLockInfoScript.script(), List.class, null,
                    loadNodeLockInfoScript.scriptKey(coordinatorConfig.getLeaderKey(),
                            followerId + "-" + coordinatorConfig.getLockWaitSuffix(),
                            followerId + "-" + coordinatorConfig.getLockListSuffix()),
                    loadNodeLockInfoScript.scriptArgv(CoordinatorConst.idStr, leader.getId(), coordinatorConfig.getReadLockSuffix(), coordinatorConfig.getWriteLockSuffix(),
                            coordinatorConfig.getLockWaitSuffix(), CoordinatorConst.readEnterStr, CoordinatorConst.writeEnterStr));
            Long result = Long.parseLong(list.get(0).toString());
            if(result.equals(-1L)) {
                leader.setNeedReElectionTrue();
                return;
            }
            int lockSize = Integer.parseInt(list.get(2).toString());
            NodeLockInfo nodeLockInfo = new NodeLockInfo(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
            if(list.size() > 4) {
                List lock = list.subList(4, 4 + lockSize * 2);
                for(int i = 0; i < lock.size(); i += 2) {
                    String path = lock.get(i).toString();
                    LockPathSplitter.LockSplit pathParts = splitNotifyLockPart(path);
                    String checkPath = pathParts.getPath();
                    NodeLockInfo.LockInfo lockInfo = new NodeLockInfo.LockInfo(checkPath, Integer.parseInt(lock.get(i + 1).toString()));
                    nodeLockInfo.getLocks().put(path, lockInfo);
                }
            }
            if(list.size() > 4 + lockSize * 2) {
                List wait = list.subList(4 + lockSize * 2, list.size());
                for (Object o:wait
                ) {
                    String path = o.toString();
                    LockPathSplitter.LockSplit pathParts = splitNotifyLockPart(path);
                    String checkPath = pathParts.getPath();
                    NodeLockInfo.LockWaitInfo waitInfo = new NodeLockInfo.LockWaitInfo(checkPath, 0L);
                    nodeLockInfo.getLockWait().put(path, waitInfo);
                }
            }
            lockRepairManager.reBuildLock(followerId, nodeLockInfo);
        }
        catch (Exception ex) {
            log.error("redis lock rebuild error：{}", BusinessException.exceptionFullMessage(ex));
        }
    }
}
