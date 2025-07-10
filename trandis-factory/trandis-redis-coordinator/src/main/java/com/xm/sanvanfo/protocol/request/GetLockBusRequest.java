package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class GetLockBusRequest extends CoordinatorMessageWare {
    private static final long serialVersionUID = -3442059556589107390L;

    @Override
    public boolean toServer() {
        return false;
    }

    @Override
    public boolean timeoutResend() {
        return false;
    }
}
