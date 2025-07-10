package com.xm.sanvanfo.trandiscore.globallock.LockManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface MultiLockManager {
    void acquire(List<String> paths) throws Exception;
    boolean acquire(List<String> paths, Long time, TimeUnit unit) throws Exception;
    void release(List<String> paths) throws Exception;
}
