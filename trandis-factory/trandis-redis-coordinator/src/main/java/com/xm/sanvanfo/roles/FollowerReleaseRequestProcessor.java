package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;

@SuppressWarnings("unused")
class FollowerReleaseRequestProcessor extends AbstractFollowerRequestProcessor implements IDefaultResponse {

    FollowerReleaseRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare body) {
        asyncSend(message, body, CoordinatorConst.releaseRequestPlugin, () ->defaultFailReleaseResponse(follower, (ReleaseRequest) body, "send message error"
                + body.toString()));
        return AbstractRole.ProcessStatus.OK;
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        return defaultFailReleaseResponse(follower, (ReleaseRequest) messageWare, message);
    }
}
