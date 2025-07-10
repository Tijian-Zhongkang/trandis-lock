package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.NotifyRequest;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.NotifyResponse;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class FollowerNotifyRequestProcessor extends AbstractFollowerRequestProcessor {

    FollowerNotifyRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare messageWare) {
        NotifyRequest body = (NotifyRequest)messageWare;
        String threadId = body.getNotifyThreadId();
        boolean ret = LockMessageBus.INSTANCE().notifyAll(threadId);
        if(!ret) {
            log.info("thread {} is not wait notify info is {}", threadId, body.toString());
        }
        else {
            log.debug("thread {} is wait notify info is {}", threadId, body.toString());
        }
        NotifyResponse response = new NotifyResponse();
        response.setRequest(body);
        response.setAppName(follower.getConfig().getAppName());
        response.setCoordinatorId(follower.getId());
        response.setCode(ret ? BaseResponse.ResponseCode.SUCCESS.getCode() : BaseResponse.ResponseCode.TIMEOUT.getCode());
        response.setMsg("");
        response.setId(body.getId());
        response.setDestCoordinatorId(follower.getClient().getBootstrapServerInfo().getId());
        follower.getClient().sendAsyncResponse(message.getMessage(),follower.buildPluginResponse(response, CoordinatorConst.notifyResponsePlugin));
        return AbstractRole.ProcessStatus.OK;
    }
}
