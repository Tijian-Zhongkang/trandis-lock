package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
abstract class AbstractFollowerRequestProcessor implements IRequestProcessor {
    protected final Follower follower;
    AbstractFollowerRequestProcessor(IRole role) {
        Asserts.isTrue(role instanceof Follower);
        follower = (Follower)role;
    }

    void asyncSend(CoordinatorMessage message, CoordinatorMessageWare messageWare, String plugin, Supplier<PluginResponse> supplier) {
        PluginRequest request = new PluginRequest();
        request.setPlugin(plugin);
        messageWare.setDestCoordinatorId(follower.getClient().getBootstrapServerInfo().getId());
        request.setObj(messageWare);
        try {
            follower.getClient().sendAsyncRequest(request, (o, v) -> follower.sendRequestFail(message, o, v.getRpcMessage().getBody(), supplier));
        }
        catch (BusinessException ex) {
            follower.sendRequestFail(message, follower.getClient().getNettyChannel(), request, supplier);
        }
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        throw new BusinessException(String.format("%s does has default response", this.getClass().getCanonicalName()));
    }


}
