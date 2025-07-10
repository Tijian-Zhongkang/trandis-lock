package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;

import com.xm.sanvanfo.trandiscore.globallock.LockManager.SingleReadWriteLockManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;

import java.util.concurrent.TimeUnit;

public class GlobalSingleReadWriteLockManager extends GlobalAbstractLockManager<InterProcessReadWriteLock> implements SingleReadWriteLockManager {


    GlobalSingleReadWriteLockManager(CuratorFramework framework, String namespace, String workspace) {
        super(framework, namespace, workspace);
    }

    @Override
    public void readAcquire(String path) throws Exception {
        getSyncInterProcessLock(path).readLock().acquire();
    }

    @Override
    public void writeAcquire(String path) throws Exception {
        getSyncInterProcessLock(path).writeLock().acquire();
    }

    @Override
    public boolean readAcquire(String path, long timeout, TimeUnit timeUnit) throws Exception {
        return getSyncInterProcessLock(path).readLock().acquire(timeout, timeUnit);
    }

    @Override
    public boolean writeAcquire(String path, long timeout, TimeUnit timeUnit) throws Exception {
        return getSyncInterProcessLock(path).writeLock().acquire(timeout, timeUnit);
    }

    public void readRelease(String path) throws Exception {
        getSyncInterProcessLock(path).readLock().release();
        checkDeleteNode(path);
    }

    public void writeRelease(String path) throws Exception {
        getSyncInterProcessLock(path).writeLock().release();
        checkDeleteNode(path);
    }

    @Override
    protected InterProcessReadWriteLock createInterProcessLock(String wrapPath) {
        return new InterProcessReadWriteLock(curatorFramework, wrapPath);
    }
}
