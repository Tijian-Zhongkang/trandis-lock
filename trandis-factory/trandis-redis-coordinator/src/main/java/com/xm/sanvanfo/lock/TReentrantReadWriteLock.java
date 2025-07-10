package com.xm.sanvanfo.lock;

import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import com.xm.sanvanfo.protocol.ReentrantLockPath;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class TReentrantReadWriteLock extends TAbstractReentrantLock {

    private final ThreadLocal<Integer> reentrantReadTimes = new ThreadLocal<>();
    private final ThreadLocal<Integer> reentrantWriteTimes = new ThreadLocal<>();

    public TReentrantReadWriteLock(String path) {
        super(path);
        reentrantReadTimes.set(0);
        reentrantWriteTimes.set(0);
    }

    public void readLock() throws Exception {

        readLock(-1L, null);
    }

    public boolean readLock(Long timeout, TimeUnit timeUnit) throws Exception {
        Integer times = getReentrantTimes(reentrantReadTimes);
        times++;
        AcquireRequest acquire = new AcquireRequest();
        acquire.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        ReentrantLockPath path = new ReentrantLockPath(times, timeout, timeUnit, Collections.singletonList(this.path));
        acquire.setReadPath(path);
       boolean ret =  acquire(acquire, timeout, timeUnit);
       if(ret) {
           reentrantReadTimes.set(times);
       }
       return ret;
    }

    public void unlockRead() throws Exception {
        Integer times = reentrantWriteTimes.get();
        times--;
        if(times < 0) {
            times = 0;
        }
        ReleaseRequest release = new ReleaseRequest();
        release.setThreadId(CoordinatorThreadMessageWare.getQuoteThreadId());
        release.setReadPath(Collections.singletonList(this.path));
        release.setReadEntrantTimes(times);
        super.release(release);
        reentrantReadTimes.set(times);
    }

    public void writeLock() throws Exception {
        writeLock(-1L, null);
    }

    public boolean writeLock(Long timeout, TimeUnit timeUnit) throws Exception {
        Integer times = getReentrantTimes(reentrantWriteTimes);
        times++;
        boolean ret = super.writeLock(times, timeout, timeUnit);
        if(ret) {
            reentrantWriteTimes.set(times);
        }
        return ret;
    }

    public void unlockWrite() throws Exception {
        Integer times = reentrantWriteTimes.get();
        times--;
        if(times < 0) {
            times = 0;
        }
        writeRelease(times);
        reentrantWriteTimes.set(times);
    }
}
