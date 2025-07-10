package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.protocol.request.BranchLockResourceRequest;
import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_LOCK_RESOURCE_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchLockResourceResponse extends  BranchAbstractResponse {

    private static final long serialVersionUID = -6202483436043826377L;

    private Map<Boolean, ReadWriteLockPath.LockPath> readWrite;
    private BranchLockResourceRequest.LockType lockType;
    private String lockId;

    @Override
    public MessageType getMessageType() {
        return TYPE_LOCK_RESOURCE_RESPONSE;
    }
}
