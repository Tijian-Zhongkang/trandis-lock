package com.xm.sanvanfo.protocol.response;

import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.roles.NodeLockInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class AcquireResponse extends BaseResponse {
    private static final long serialVersionUID = -4795574833190954254L;
    private String coordinatorId;
    private String threadId;
    private AcquireRequest acquireRequest;
    private NodeLockInfo.LockWaitInfo waitPath;
    private List<NodeLockInfo.LockInfo> lockPath;

    @Override
    public boolean toServer() {
        return false;
    }
}
