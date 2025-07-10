package com.xm.sanvanfo.lock;


import java.util.concurrent.TimeUnit;

@SuppressWarnings({"UnusedReturnValue"})
public class TReentrantLock extends TAbstractReentrantLock {

    private final ThreadLocal<Integer> reentrantTimes = new ThreadLocal<>();

    public TReentrantLock(String path) {
        super(path);
        reentrantTimes.set(0);
    }

    public void acquire() throws Exception {
        acquire(-1L, null);
    }

    public boolean acquire(Long timeout, TimeUnit timeUnit) throws Exception {
        Integer times = getReentrantTimes(reentrantTimes);
        times++;
        boolean ret = writeLock(times, timeout, timeUnit);
        if(ret) {
            reentrantTimes.set(times);
        }
        return ret;
    }

    public void release() throws Exception {
        Integer times = reentrantTimes.get();
        times--;
        if(times < 0) {
            times = 0;
        }
       writeRelease(times);
        reentrantTimes.set(times);
    }
}
