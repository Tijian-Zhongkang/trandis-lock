package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_LOCK_RESOURCE_REQUEST;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchLockResourceRequest extends  BranchAbstractRequest {

    public enum LockType{
        Lock(0),
        ReleaseRead(1),
        Release(2);

        private int code;

        LockType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LockType get(int code) {
            return LockType.values()[code];
        }
    }

    private static final long serialVersionUID = -7265814191759106862L;

    private Map<Boolean, ReadWriteLockPath.LockPath> readWrite;
    private LockType lockType;
    private String lockId;

    @Override
    public MessageType getMessageType() {
        return TYPE_LOCK_RESOURCE_REQUEST;
    }
}
