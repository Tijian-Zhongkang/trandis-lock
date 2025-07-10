package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DefaultRequestProcessor implements IRequestProcessor {

    private final IRole role;

    DefaultRequestProcessor(IRole role) {
        this.role = role;
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare messageWare) {
        log.info("default process request {}, role type is {}", message.toString(), role.getRole());
        return AbstractRole.ProcessStatus.OK;
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message) {
        throw new BusinessException("com.xm.sanvanfo.roles.DefaultRequestProcessor does has default response");
    }
}
