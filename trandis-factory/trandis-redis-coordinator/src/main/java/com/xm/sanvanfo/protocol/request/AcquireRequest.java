package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import com.xm.sanvanfo.protocol.ReentrantLockPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class AcquireRequest extends CoordinatorThreadMessageWare {
    private static final long serialVersionUID = 9048784966846561556L;
    private ReentrantLockPath readPath;
    private ReentrantLockPath writePath;

    @Override
    public boolean toServer() {
        return true;
    }

    @Override
    public boolean timeoutResend() {
        return true;
    }
}
