package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;

public interface IRequestAsyncProcessor {
    AbstractRole.ProcessStatus processAsync(CoordinatorMessage message, CoordinatorMessageWare body);
}
