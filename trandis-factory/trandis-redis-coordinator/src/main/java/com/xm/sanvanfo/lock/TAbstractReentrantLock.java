package com.xm.sanvanfo.lock;

import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import com.xm.sanvanfo.protocol.ReentrantLockPath;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

abstract class TAbstractReentrantLock extends TAbstractLock {

    final String path;

    TAbstractReentrantLock(String path) {
        this.path = path;
    }

    boolean writeLock(Integer times ,Long timeout, TimeUnit timeUnit) throws Exception {
        AcquireRequest acquire = new AcquireRequest();
        acquire.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        ReentrantLockPath path = new ReentrantLockPath(times, timeout, timeUnit, Collections.singletonList(this.path));
        acquire.setWritePath(path);
        return acquire(acquire, timeout, timeUnit);
    }

    void writeRelease(Integer times) throws Exception {
        ReleaseRequest release = new ReleaseRequest();
        release.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        release.setWriteEntrantTimes(times);
        release.setWritePath(Collections.singletonList(this.path));
        release(release);
    }

    Integer getReentrantTimes(ThreadLocal<Integer> local) {
        Integer times = local.get();
        if(null == times) {
            local.set(0);
            times = 0;
        }
        return times;
    }
}
