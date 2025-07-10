package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
@Data
class FollowerInfo {
    private NettyChannel channel;
    private Long updateTime;
    private List<ResponseMessage> failMessages;
    private List<PluginRequest> failRequests;
    private ReentrantLock lock;    //use reentrant lock no readwrite lock, because send fail will need write lock but thread maybe obtain read lock
    private volatile InitStatus init;
    private NodeLockInfo nodeLockInfo;

    enum InitStatus {
        NoInit,
        NoInitRepairing,
        Initing,
        Inited
    }
}
