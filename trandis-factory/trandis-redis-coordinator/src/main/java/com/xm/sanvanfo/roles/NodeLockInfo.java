package com.xm.sanvanfo.roles;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@AllArgsConstructor
@Data
public class NodeLockInfo {
    private final Map<String, LockInfo> locks;
    private final Map<String, LockWaitInfo> lockWait;

    private NodeLockInfo() {
        locks = new ConcurrentHashMap<>();
        lockWait = new ConcurrentHashMap<>();
    }

    NodeLockInfo deepCopy() {
        NodeLockInfo nodeLockInfo = new NodeLockInfo();
        nodeLockInfo.getLocks().putAll(getLocks());
        nodeLockInfo.getLockWait().putAll(getLockWait());
        return nodeLockInfo;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LockInfo  {
        private String path;
        private Integer reEnter;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LockWaitInfo {
        private String path;
        private Long addTime;
    }

}
