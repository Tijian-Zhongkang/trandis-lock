package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;

import com.xm.sanvanfo.common.utils.LruThreadSafeGetter;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.MultiLockManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMultiLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class GlobalMultiLockManager extends GlobalAbstractLockManager<InterProcessMutex> implements MultiLockManager {


    private final LruThreadSafeGetter<List<String>, InterProcessMultiLock> multiLocks = new LruThreadSafeGetter<>
            (3600*24L, 30L, o-> {
                o = o.stream().distinct().sorted(String::compareTo).collect(Collectors.toList());
                return createGlobalInterProcessMultiLock(o);
            });

    GlobalMultiLockManager(CuratorFramework framework, String namespace, String workspace) {
        super(framework, namespace, workspace);
    }

    @Override
    public void acquire(List<String> paths) throws Exception {

        InterProcessMultiLock lock = getGlobalInterProcessMultiLock(paths);
        lock.acquire();
     }

     @Override
     public boolean acquire(List<String> paths, Long time, TimeUnit unit) throws Exception {
         InterProcessMultiLock lock = getGlobalInterProcessMultiLock(paths);
         return lock.acquire(time, unit);
     }

     @Override
     public void release(List<String> paths) throws Exception {
         InterProcessMultiLock lock = getGlobalInterProcessMultiLock(paths);
         lock.release();
         for (String path:paths
              ) {
             checkDeleteNode(path);
         }
     }

    @Override
    public void dispose() {
        super.dispose();
        multiLocks.dispose();
    }

    @Override
    protected InterProcessMutex createInterProcessLock(String wrapPath) {
        return new InterProcessMutex(curatorFramework, wrapPath);
    }

    private InterProcessMultiLock getGlobalInterProcessMultiLock(List<String> paths) {
        return multiLocks.get(paths);
    }

    private InterProcessMultiLock createGlobalInterProcessMultiLock(List<String> paths) {
        List<InterProcessLock> mutexes = new ArrayList<>();
        for (String path:paths
        ) {
            mutexes.add(getSyncInterProcessLock(path));
        }
        return new InterProcessMultiLock(mutexes);
    }
}
