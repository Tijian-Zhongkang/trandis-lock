package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.HeartBeatRequest;
import com.xm.sanvanfo.protocol.response.HeartBeatResponse;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;

@SuppressWarnings("unused")
class LeaderHeartBeatRequestProcessor extends AbstractLeaderRequestProcessor {

    LeaderHeartBeatRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare messageWare) {
        HeartBeatRequest body = (HeartBeatRequest)messageWare;
        AbstractNettyRemoting remoting = leader.getCoordinator().getNodeInfo().getRemoting();
        Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
        AbstractNettyRemotingServer server = (AbstractNettyRemotingServer)remoting;
        HeartBeatResponse response = new HeartBeatResponse();
        response.setServerId(server.getServerId());
        response.setCoordinatorId(leader.getId());
        response.setDestCoordinatorId(body.getCoordinatorId());
        response.setId(body.getId());
        response.setAppName(leader.getConfig().getAppName());
        response.setCode(200);
        response.setMsg("");
        asyncSendResponse(message, body, response, CoordinatorConst.heartBeatResponsePlugin);
        return AbstractRole.ProcessStatus.OK;
    }
}
