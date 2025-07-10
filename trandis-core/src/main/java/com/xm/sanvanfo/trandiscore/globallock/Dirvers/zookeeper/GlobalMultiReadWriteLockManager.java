package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.globallock.LockManager.MultiReadWriteLockManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.reverse;

@Slf4j
public class GlobalMultiReadWriteLockManager extends GlobalAbstractLockManager<InterProcessReadWriteLock> implements MultiReadWriteLockManager {


    GlobalMultiReadWriteLockManager(CuratorFramework framework, String namespace, String workspace) {
        super(framework, namespace, workspace);
    }

    //Atomic lock read write
    @Override
    public void acquire(List<String> read, List<String> write) throws Exception {
       acquire(read, -1, null, write, -1, null);
    }

    //Atomic lock read write
    @Override
    public Boolean acquire(List<String> read, long readTimeout, TimeUnit readTimeUnit,
                        List<String> write, long writeTimeout, TimeUnit writeTimeUnit) throws Exception {
        if(null != write && write.size() > 0) {
            write.sort(String::compareTo);
            boolean ret = lockAcquireInner(write, writeTimeout, writeTimeUnit, InterProcessReadWriteLock::writeLock);
            if(!ret) {
                return false;
            }
        }
        try {
            if(null != read && read.size() > 0) {
                read.sort(String::compareTo);
                boolean ret = lockAcquireInner(read, readTimeout, readTimeUnit, InterProcessReadWriteLock::readLock);
                if(!ret) {
                    if(null != write && write.size() > 0) {
                        readRelease(write);
                    }
                    return false;
                }
            }
            return true;
        }
        catch (Exception ex) {
            log.warn(String.format("read lock exception roll back write lock %s", ex.getMessage()));
            if(null != write && write.size() > 0) {
                readRelease(write);
            }
            throw ex;
        }
    }

    @Override
    public void release(List<String> read, List<String> write) throws Exception {


        if(null != read && read.size() > 0) {
            read.sort(String::compareTo);
            readRelease(reverse(read));
        }
        if(null != write && write.size() > 0) {
            write.sort(String::compareTo);
            writeRelease(reverse(write));
        }
    }

    @Override
    public void readAcquire(List<String> paths) throws Exception {
        lockAcquire(paths, InterProcessReadWriteLock::readLock);
    }

    @Override
    public void writeAcquire(List<String> paths) throws Exception {
        lockAcquire(paths, InterProcessReadWriteLock::writeLock);
    }


    @Override
    public boolean readAcquire(List<String> paths, long timeout, TimeUnit timeUnit) throws Exception {
        return lockAcquire(paths, timeout, timeUnit, InterProcessReadWriteLock::readLock);
    }

    @Override
    public boolean writeAcquire(List<String> paths, long timeout, TimeUnit timeUnit) throws Exception {
        return lockAcquire(paths, timeout, timeUnit, InterProcessReadWriteLock::writeLock);
    }

    @Override
    public void readRelease(List<String> paths) throws Exception {
        if(null == paths || paths.size() == 0) {
            throw new BusinessException("read release path is empty");
        }
        paths = paths.stream().distinct().collect(Collectors.toList());
        for (String path:reverse(paths)
             ) {
            log.debug(String.format("release read lock %s", path));
            getSyncInterProcessLock(path).readLock().release();
            checkDeleteNode(path);
        }
    }

    @Override
    public void writeRelease(List<String> paths) throws Exception {
        if(null == paths || paths.size() == 0) {
            throw new BusinessException("write release path is empty");
        }
        paths = paths.stream().distinct().collect(Collectors.toList());
        for (String path:reverse(paths)
             ) {
            log.debug(String.format("release write lock %s", path));
            getSyncInterProcessLock(path).writeLock().release();
            checkDeleteNode(path);
        }
    }

    @Override
    protected InterProcessReadWriteLock createInterProcessLock(String wrapPath) {
        return new InterProcessReadWriteLock(curatorFramework, wrapPath);
    }

    private void lockAcquire(List<String> paths, Function<InterProcessReadWriteLock, InterProcessMutex> func) throws Exception {
        lockAcquire(paths, -1, null, func);
    }

    private boolean lockAcquire(List<String> paths, long timeout, TimeUnit unit, Function<InterProcessReadWriteLock, InterProcessMutex> func) throws Exception {
        if(null == paths || paths.size() == 0) {
            throw new BusinessException("acquire list is empty");

        }
        paths.sort(String::compareTo);
        return lockAcquireInner(paths, timeout, unit, func);

    }

    private boolean lockAcquireInner(List<String> paths, long timeout, TimeUnit unit, Function<InterProcessReadWriteLock, InterProcessMutex> func) throws Exception {

        boolean acquire = true;
        Exception exception = null;
        List<InterProcessReadWriteLock> success = new ArrayList<>();
        paths = paths.stream().distinct().collect(Collectors.toList());
        for (String path:paths
        ) {
            try {
                long begin = System.currentTimeMillis();
                InterProcessReadWriteLock lock = getSyncInterProcessLock(path);
                if(timeout == -1) {
                    func.apply(lock).acquire();
                }
                else {
                    acquire = func.apply(lock).acquire(timeout, unit);
                    if(!acquire) {
                        break;
                    }
                }
                long end = System.currentTimeMillis();
                log.debug((end - begin) + " ms -------- path:" + path);
                success.add(lock);
            }
            catch (Exception ex) {
                log.warn(String.format("read acquire %s error: %s", path, ex.getMessage()));
                acquire = false;
                exception = ex;
                break;
            }
        }
        if(!acquire) {
            for (InterProcessReadWriteLock lock:success
            ) {
                func.apply(lock).release();
            }
        }
        if(null != exception) {
            throw  exception;
        }
        return acquire;
    }
}
