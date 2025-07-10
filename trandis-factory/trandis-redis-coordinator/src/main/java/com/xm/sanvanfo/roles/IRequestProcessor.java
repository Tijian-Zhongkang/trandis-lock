package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;

interface IRequestProcessor {
    AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare messageWare) throws Exception;

    PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message);
}
