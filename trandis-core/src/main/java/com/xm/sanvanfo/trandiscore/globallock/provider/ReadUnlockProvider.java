package com.xm.sanvanfo.trandiscore.globallock.provider;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReadUnlockProvider implements GlobalLockProvider {

    private final GlobalLockManager manager;

    public ReadUnlockProvider(GlobalLockManager manager) {
        this.manager = manager;
    }

    @Override
    public void acquire(List<String> readPaths, List<String> writePaths)  throws Exception {
        if(null == writePaths || writePaths.size() == 0) {
            return;
        }
        manager.getMultiLockManager().acquire(writePaths);
    }

    @Override
    public Boolean acquire(List<String> readPaths, Long readTimeout, TimeUnit readTimeUnit,
                                 List<String> writePaths, Long writeTimeout, TimeUnit writeUnit)  throws Exception {
        if(null == writePaths || writePaths.size() == 0) {
            return true;
        }
        return  manager.getMultiLockManager().acquire(writePaths, writeTimeout, writeUnit);
    }

    @Override
    public void release(List<String> readPaths, List<String> writePaths)  throws Exception {
        if(null == writePaths || writePaths.size() == 0) {
            return;
        }
        manager.getMultiLockManager().release(writePaths);
    }

    @Override
    public List<String> releaseRead(List<String> read, List<String> write) throws Exception {
        return read;
    }

}
