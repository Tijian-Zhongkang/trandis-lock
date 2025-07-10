package com.xm.sanvanfo.lock;

import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import com.xm.sanvanfo.protocol.ReentrantLockPath;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"UnusedReturnValue"})
public class TMultiReadWriteLock extends TAbstractLock {

    private final List<String> readPaths;
    private final List<String> writePaths;

    public TMultiReadWriteLock(List<String> readPaths, List<String> writePaths) {
        this.readPaths = readPaths;
        this.writePaths = writePaths;
        this.readPaths.sort(String::compareTo);
        this.writePaths.sort(String::compareTo);
    }

    public void acquire() throws Exception {
        acquire(-1L, null);
    }

    public boolean acquire(Long timeout, TimeUnit timeUnit) throws Exception {
        AcquireRequest request = new AcquireRequest();
        request.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        ReentrantLockPath readPath = new ReentrantLockPath(-1, timeout, timeUnit, readPaths);
        ReentrantLockPath writePath = new ReentrantLockPath(-1, timeout, timeUnit, writePaths);
        request.setReadPath(readPath);
        request.setWritePath(writePath);
        return super.acquire(request, timeout, timeUnit);
    }

    public void release() throws Exception {
        ReleaseRequest request = new ReleaseRequest();
        request.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        request.setReadEntrantTimes(-1);
        request.setReadPath(readPaths);
        request.setWriteEntrantTimes(-1);
        request.setWritePath(writePaths);
        super.release(request);
    }
}
