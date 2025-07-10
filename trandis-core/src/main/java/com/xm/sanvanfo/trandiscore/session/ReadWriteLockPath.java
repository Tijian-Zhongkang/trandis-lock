package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchLockResourceRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class ReadWriteLockPath {
    private NettyChannel channel;
    private RpcMessage rpcMessage;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LockPath implements Serializable {
        private static final long serialVersionUID = -3667192830651213370L;
        private List<String> path;
        private Long timeout;
        private TimeUnit timeUnit;
    }

    Map<Boolean, LockPath> getReadWrite() {
        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof BranchLockResourceRequest);
        return ((BranchLockResourceRequest)obj).getReadWrite();
    }

    String getLockId() {
        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof BranchLockResourceRequest);
        return ((BranchLockResourceRequest)obj).getLockId();
    }

    BranchLockResourceRequest.LockType getLockType() {
        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof BranchLockResourceRequest);
        return ((BranchLockResourceRequest)obj).getLockType();
    }

    @SuppressWarnings("unused")
    public ReadWriteLockPath() {}

    public ReadWriteLockPath(RpcMessage rpcMessage, NettyChannel channel) {
        this.rpcMessage = rpcMessage;
        this.channel = channel;
    }
}
