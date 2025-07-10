package com.xm.sanvanfo.trandiscore.globallock.LockManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface MultiReadWriteLockManager {

    void acquire(List<String> read, List<String> write) throws Exception;
    Boolean acquire(List<String> read, long readTimeout, TimeUnit readTimeUnit,
                           List<String> write, long writeTimeout, TimeUnit writeTimeUnit) throws Exception;
    void release(List<String> read, List<String> write) throws Exception;
    void readAcquire(List<String> paths) throws Exception;
    void writeAcquire(List<String> paths) throws Exception;
    boolean readAcquire(List<String> paths, long timeout, TimeUnit timeUnit) throws Exception;
    boolean writeAcquire(List<String> paths, long timeout, TimeUnit timeUnit) throws Exception;
    void readRelease(List<String> paths) throws Exception;
    void writeRelease(List<String> paths) throws Exception;

}
