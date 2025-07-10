package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.SupplierException;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

@Slf4j
abstract class AbstractLeaderRequestProcessor implements IRequestProcessor {
    protected final Leader leader;

    AbstractLeaderRequestProcessor(IRole role) {
        Asserts.isTrue(role instanceof Leader);
        leader = (Leader)role;
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        throw new BusinessException(String.format("%s does has default response", this.getClass().getCanonicalName()));
    }

    void asyncSendResponse(CoordinatorMessage message, CoordinatorMessageWare body, CoordinatorMessageWare response, String pluginName) {

        if(!body.getCoordinatorId().equals(leader.getId())) {
            FollowerInfo info = leader.getFollowersMap().get(body.getCoordinatorId());
            if(null == info) {
                log.warn("async send follower delete by leader follower id is {}", body.getCoordinatorId());
            }
            else {
                NettyChannel channel = info.getChannel();
                AbstractNettyRemoting remoting = leader.getCoordinator().getNodeInfo().getRemoting();
                Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
                AbstractNettyRemotingServer server = (AbstractNettyRemotingServer) remoting;
                leader.asyncSendResponse(server, message.getMessage(), leader.buildPluginResponse(response, pluginName), channel,
                        body.getCoordinatorId());
            }
        }
        else {
            if(StringUtils.isNotEmpty(response.getId())) {
                leader.getCoordinator().processResponse(leader.buildPluginResponse(response, pluginName));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    <T> T followerLockProcess(SupplierException<T> func, Function<FollowerInfo, Boolean> hasRetry, CoordinatorMessageWare body, T notLock, T fail) throws Exception {
        FollowerInfo info = null;
        if(!body.getCoordinatorId().equals(leader.getId())) {
            info = leader.getFollowersMap().get(body.getCoordinatorId());
            if (null == info) {
                return notLock;
            }
            info.getLock().lock();
        }
        try {
            if( null != info && !leader.getFollowersMap().containsKey(body.getCoordinatorId())) {
                return notLock;
            }
            if(leader.isNotInit(body.getCoordinatorId())) {
                log.info("{} node id {} is not init  data {}", body.getClass().getCanonicalName(), body.getCoordinatorId(), body.toString());
                return fail;
            }
            if(null != hasRetry && hasRetry.apply(info)) {
                log.info("{}  node id {} has retry task  data {}", body.getClass().getCanonicalName(), body.getCoordinatorId(), body.toString());
                return fail;
            }
            return func.get();
        }
        finally {
            if(null != info) {
                info.getLock().unlock();
            }
        }
    }

}
