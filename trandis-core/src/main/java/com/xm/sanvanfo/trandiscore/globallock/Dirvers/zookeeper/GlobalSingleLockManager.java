package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;


import com.xm.sanvanfo.trandiscore.globallock.LockManager.SingleLockManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;


import java.util.concurrent.TimeUnit;

@Slf4j
public class GlobalSingleLockManager extends GlobalAbstractLockManager<InterProcessMutex> implements SingleLockManager {


    GlobalSingleLockManager(CuratorFramework framework, String namespace, String workspace) {
        super(framework, namespace, workspace);
    }

    @Override
    protected InterProcessMutex createInterProcessLock(String wrapPath) {
        return new InterProcessMutex(curatorFramework, wrapPath);
    }

    @Override
    public void acquire(String path) throws Exception {
        getSyncInterProcessLock(path).acquire();
    }

    @Override
    public boolean acquire(String path, Long time, TimeUnit unit) throws Exception {
        return getSyncInterProcessLock(path).acquire(time, unit);
    }

    @Override
    public void release(String path) throws Exception {
        getSyncInterProcessLock(path).release();
        checkDeleteNode(path);
    }



}
