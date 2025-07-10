package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.CloseRequest;

@SuppressWarnings("unused")
class LeaderCloseRequestProcessor extends AbstractLeaderRequestProcessor {

    LeaderCloseRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare messageWare) {
        CloseRequest body = (CloseRequest)messageWare;
        ObjectHolder<Boolean> objectHolder = new ObjectHolder<>();
        leader.deleteFollower(body.getCoordinatorId(), true, objectHolder, true);
        return AbstractRole.ProcessStatus.OK;
    }
}
