package com.xm.sanvanfo.trandiscore.globallock.LockManager;

import java.util.concurrent.TimeUnit;

public interface SingleLockManager {
    void acquire(String path) throws Exception;
    boolean acquire(String path, Long time, TimeUnit unit) throws Exception;
    void release(String path) throws Exception;
}
