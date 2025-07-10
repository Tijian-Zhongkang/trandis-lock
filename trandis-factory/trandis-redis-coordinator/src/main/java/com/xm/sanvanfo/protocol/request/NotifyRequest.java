package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class NotifyRequest extends CoordinatorMessageWare{

    private static final long serialVersionUID = 109756628808830786L;
    private String notifyCoordinatorId;
    private String notifyThreadId;
    private String path;
    private Boolean read;

    @Override
    public boolean toServer() {
        return false;
    }

    @Override
    public boolean timeoutResend() {
        return true;
    }
}
