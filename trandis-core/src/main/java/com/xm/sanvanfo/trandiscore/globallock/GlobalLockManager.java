package com.xm.sanvanfo.trandiscore.globallock;

import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.globallock.Dirvers.LockDriver;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.*;
import com.xm.sanvanfo.trandiscore.globallock.provider.ReadLockProvider;
import com.xm.sanvanfo.trandiscore.globallock.provider.ReadUnlockProvider;
import com.xm.sanvanfo.trandiscore.globallock.provider.SerializableLockProvider;
import com.xm.sanvanfo.trandiscore.globallock.provider.UnlockProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GlobalLockManager {

    private static GlobalLockManager instance = null;
    private final  ConcurrentHashMap<TransactionLevelEx, GlobalLockProvider> providers = new ConcurrentHashMap<>();
    private LockDriver driver;

    public static void init(LockDriver driver) {
        if(instance != null) {
            return;
        }
        synchronized(GlobalLockManager.class)  {
            if(instance != null) {
                return;
            }
            instance = new GlobalLockManager();
            instance.driver = driver;
        }
    }

    public static GlobalLockManager INSTANCE() {

        return instance;
    }

    public GlobalLockConfiguration config() {
        return driver.getConfig();
    }

    public void acquire(TransactionLevelEx level, List<String> readPaths, List<String> writePaths) throws Exception {
        if(null == readPaths && null == writePaths) {
            return;
        }
        getProvider(level).acquire(readPaths, writePaths);
    }

    public Boolean acquire(TransactionLevelEx level, List<String> readPaths, Long readTimeout, TimeUnit readTimeUnit,
                                 List<String> writePaths, Long writeTimeout, TimeUnit writeUnit) throws Exception {
        if(null == readPaths && null == writePaths) {
            return true;
        }
         return getProvider(level).acquire(readPaths, readTimeout, readTimeUnit, writePaths, writeTimeout, writeUnit);
    }

    public void release(TransactionLevelEx level, List<String> readPaths, List<String> writePaths) throws Exception {
        if(null == readPaths && null == writePaths) {
            return;
        }
         getProvider(level).release(readPaths, writePaths);
    }

    public List<String> releaseRead(TransactionLevelEx levelEx, List<String> read, List<String> write) throws Exception {
        if(null == read) {
            return null;
        }
        return getProvider(levelEx).releaseRead(read, write);
    }

    public SingleLockManager getSingleLockManager() {
        return driver.getSingleLockManager();
    }

    public MultiLockManager getMultiLockManager() {
        return driver.getMultiLockManager();
    }

    @SuppressWarnings({"unused"})
    public SingleReadWriteLockManager getSingleReadWriteLockManager() { return driver.getSingleReadWriteLockManager();}

    public MultiReadWriteLockManager getMultiReadWriteLockManager() {return driver.getMultiReadWriteLockManager();}


    private GlobalLockProvider getProvider(TransactionLevelEx level) {

        return providers.computeIfAbsent(level, this::createLevelProvider);
    }

    private GlobalLockProvider createLevelProvider(TransactionLevelEx level) {
        GlobalLockProvider provider;
        switch (level) {
            case ReadUnCommitted:
                provider = new ReadUnlockProvider(instance);
                break;
            case Serializable:
                provider = new SerializableLockProvider(instance);
                break;
            case Unlock:
                provider = new UnlockProvider();
                break;
            default:
                provider = new ReadLockProvider(instance);
                break;
        }
        return provider;
    }

}
