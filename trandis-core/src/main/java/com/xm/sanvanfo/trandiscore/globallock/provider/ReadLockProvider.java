package com.xm.sanvanfo.trandiscore.globallock.provider;

import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReadLockProvider implements GlobalLockProvider {

    private final  GlobalLockManager manager;

    public ReadLockProvider(GlobalLockManager manager) {
        this.manager = manager;
    }

    @Override
    public void acquire(List<String> readPaths, List<String> writePaths)  throws Exception {
        manager.getMultiReadWriteLockManager().acquire(readPaths, writePaths);
    }

    @Override
    public Boolean acquire(List<String> readPaths, Long readTimeout, TimeUnit readTimeUnit,
                                 List<String> writePaths, Long writeTimeout, TimeUnit writeUnit)  throws Exception {
        return manager.getMultiReadWriteLockManager().acquire(readPaths, readTimeout, readTimeUnit, writePaths, writeTimeout, writeUnit);
    }

    @Override
    public void release(List<String> readPaths, List<String> writePaths)  throws Exception {
         manager.getMultiReadWriteLockManager().release(readPaths, writePaths);
    }

    @Override
    public List<String> releaseRead(List<String> read, List<String> write) throws Exception {
        manager.getMultiReadWriteLockManager().readRelease(read);
        return read;
    }

}
