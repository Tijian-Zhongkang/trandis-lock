package com.xm.sanvanfo.trandiscore.globallock.provider;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SerializableLockProvider implements GlobalLockProvider {

    private final GlobalLockManager manager;

    public SerializableLockProvider(GlobalLockManager manager) {
        this.manager = manager;
    }

    @Override
    public void acquire(List<String> readPaths, List<String> writePaths)  throws Exception {

        writePaths = combineReadWrite(readPaths, writePaths);
        if(writePaths.size() == 0) {
            throw new BusinessException("readPaths and writePaths are empty");
        }
        manager.getMultiLockManager().acquire(writePaths);
    }

    @Override
    public Boolean acquire(List<String> readPaths, Long readTimeout, TimeUnit readTimeUnit,
                                 List<String> writePaths, Long writeTimeout, TimeUnit writeUnit)  throws Exception {

        writePaths = combineReadWrite(readPaths, writePaths);
        if(writePaths.size() == 0) {
            throw new BusinessException("readPaths and writePaths are empty");
        }
        return manager.getMultiLockManager().acquire(writePaths, writeTimeout, writeUnit);
    }

    @Override
    public void release(List<String> readPaths, List<String> writePaths)  throws Exception {
        if(null == writePaths) {
            writePaths = new ArrayList<>();
        }
        if(readPaths != null) {
            writePaths.addAll(readPaths);
        }
        writePaths = writePaths.stream().distinct().collect(Collectors.toList());
        manager.getMultiLockManager().release(writePaths);
    }

    @Override
    public List<String> releaseRead(List<String> read, List<String> write) throws Exception {
        return null;
    }


    private List<String> combineReadWrite(List<String> readPaths, List<String> writePaths) {
        List<String> combine = new ArrayList<>();
        if(null != writePaths) {
            combine.addAll(writePaths);
        }
        if(readPaths != null) {
            combine.addAll(readPaths);
        }
        return combine.stream().distinct().collect(Collectors.toList());
    }
}
