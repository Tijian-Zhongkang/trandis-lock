package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.globallock.*;
import com.xm.sanvanfo.trandiscore.globallock.Dirvers.LockDriver;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.MultiLockManager;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.MultiReadWriteLockManager;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.SingleLockManager;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.SingleReadWriteLockManager;
import com.xm.sanvanfo.trandiscore.netty.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

@Slf4j
public class ZookeeperLockDriver implements LockDriver {

    private GlobalSingleLockManager singleLockManager;
    private GlobalMultiLockManager multiLockManager;
    private GlobalSingleReadWriteLockManager singleReadWriteLockManager;
    private GlobalMultiReadWriteLockManager multiReadWriteLockManager;
    private GlobalLockConfiguration configuration;
    static boolean hasContainerSupport;

    static {
        try
        {
            CreateMode.valueOf("CONTAINER");
            hasContainerSupport = true;
        }
        catch ( IllegalArgumentException ignore )
        {
            hasContainerSupport = false;
            log.warn("The version of ZooKeeper being used doesn't support Container nodes. CreateMode.PERSISTENT will be used instead.");
        }
    }

    @Override
    public void init(GlobalLockConfiguration configuration) {
        Asserts.isTrue(CuratorFramework.class.isAssignableFrom(configuration.getFramework().getClass()));
        CuratorFramework framework = (CuratorFramework) configuration.getFramework();
        singleLockManager = new GlobalSingleLockManager(framework, configuration.getSpace(), "locks");
        multiLockManager = new GlobalMultiLockManager(framework, configuration.getSpace(), "locks");
        singleReadWriteLockManager = new GlobalSingleReadWriteLockManager(framework, configuration.getSpace(), "readwrite");
        multiReadWriteLockManager = new GlobalMultiReadWriteLockManager(framework,
                configuration.getSpace(), "readwrite");

        this.configuration = configuration;
    }

    @Override
    public SingleLockManager getSingleLockManager() {
        return singleLockManager;
    }

    @Override
    public MultiLockManager getMultiLockManager() {
        return multiLockManager;
    }

    @Override
    public SingleReadWriteLockManager getSingleReadWriteLockManager() {
        return singleReadWriteLockManager;
    }

    @Override
    public MultiReadWriteLockManager getMultiReadWriteLockManager() {
        return multiReadWriteLockManager;
    }

    @Override
    public GlobalLockConfiguration getConfig() {
        return configuration;
    }

    @Override
    public void dispose() {
        singleLockManager.dispose();
        multiLockManager.dispose();
        singleReadWriteLockManager.dispose();
        multiReadWriteLockManager.dispose();
    }
}
