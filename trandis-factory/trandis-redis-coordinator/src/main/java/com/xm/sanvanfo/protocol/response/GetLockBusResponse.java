package com.xm.sanvanfo.protocol.response;

import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.roles.NodeLockInfo;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class GetLockBusResponse extends BaseResponse {

    private static final long serialVersionUID = 2843959962790782191L;
    private Set<RpcAcquire> acquireRequests;
    private Set<RpcRelease> releaseRequests;
    private List<String> waitList;
    private NodeLockInfo nodeLockInfo;

    @Override
    public boolean toServer() {
        return true;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class RpcAcquire extends RpcCommon {
        private AcquireRequest acquireRequest;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class RpcRelease extends RpcCommon {
        private ReleaseRequest releaseRequest;
    }

    @Data
    public static class RpcCommon {
        private int id;
        private byte messageType;
        private byte codec;
        private byte compressor;
        private final Map<String, String> headMap = new HashMap<>();
        private int bodyType;
    }
}
