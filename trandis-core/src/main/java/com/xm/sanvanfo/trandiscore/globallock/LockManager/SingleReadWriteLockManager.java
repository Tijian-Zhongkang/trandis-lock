package com.xm.sanvanfo.trandiscore.globallock.LockManager;

import java.util.concurrent.TimeUnit;

public interface SingleReadWriteLockManager {

    void readAcquire(String path) throws Exception;
    void writeAcquire(String path) throws Exception;
    boolean readAcquire(String path, long timeout, TimeUnit timeUnit) throws Exception;
    boolean writeAcquire(String path, long timeout, TimeUnit timeUnit) throws Exception;
    void readRelease(String path) throws Exception;
    void writeRelease(String path) throws Exception;
}
