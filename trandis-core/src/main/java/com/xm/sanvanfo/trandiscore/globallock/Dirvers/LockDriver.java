package com.xm.sanvanfo.trandiscore.globallock.Dirvers;


import com.xm.sanvanfo.trandiscore.globallock.GlobalLockConfiguration;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.*;
import com.xm.sanvanfo.trandiscore.netty.Disposable;

public interface LockDriver extends Disposable {

    void init(GlobalLockConfiguration configuration);

    SingleLockManager getSingleLockManager();

    MultiLockManager getMultiLockManager();

    SingleReadWriteLockManager getSingleReadWriteLockManager();

    MultiReadWriteLockManager getMultiReadWriteLockManager();

    GlobalLockConfiguration getConfig();

}
