package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;

@SuppressWarnings("unused")
public class FollowerAcquireRequestProcessor extends AbstractFollowerRequestProcessor implements IDefaultResponse {

    FollowerAcquireRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare body) {
        asyncSend(message, body, CoordinatorConst.acquireRequestPlugin, ()->defaultFailAcquireResponse(follower, (AcquireRequest) body, "send message error" +
                body.toString()));
        return AbstractRole.ProcessStatus.OK;
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        return defaultFailAcquireResponse(follower, (AcquireRequest) messageWare, message);
    }
}
