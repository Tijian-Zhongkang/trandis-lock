package com.xm.sanvanfo.trandiscore.globallock.provider;

import com.xm.sanvanfo.trandiscore.globallock.GlobalLockProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UnlockProvider implements GlobalLockProvider {
    @Override
    public void acquire(List<String> readPaths, List<String> writePaths) {

    }

    @Override
    public Boolean acquire(List<String> readPaths, Long readTimeout, TimeUnit readTimeUnit, List<String> writePaths, Long writeTimeout, TimeUnit writeUnit) {
        return true;
    }

    @Override
    public void release(List<String> readPaths, List<String> writePaths) {

    }

    @Override
    public List<String> releaseRead(List<String> read, List<String> write) throws Exception {
        return read;
    }

}
