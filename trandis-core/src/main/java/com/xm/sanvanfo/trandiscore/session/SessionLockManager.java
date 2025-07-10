package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


class SessionLockManager {

    private final Map<String, ReadWriteLockPath> lockPathMap = new ConcurrentHashMap<>();
    private final Map<String, String> writeLocks = new ConcurrentHashMap<>();
    private final Map<String, String> readLocks = new ConcurrentHashMap<>();


    void filterLocks(TransactionLevelEx level, ReadWriteLockPath path) throws Exception {
        if(!lockPathMap.containsKey(path.getLockId())) {
            lockPathMap.put(path.getLockId(), path);
            filterWriteLocks(level, path.getReadWrite().get(true), path.getLockId());
            filterReadLocks(path.getReadWrite().get(false), path.getLockId());
        }

    }

    void removeReadLocks(String lockId, List<String> list) {
        ReadWriteLockPath path = lockPathMap.get(lockId);
        Optional.ofNullable(path).flatMap(o -> Optional.ofNullable(o.getReadWrite().get(false).getPath())).ifPresent(p -> {
            for (String el : list
            ) {
                readLocks.remove(el);
                p.remove(el);
            }
        });
    }

    void removeLocks(String lockId) {
        ReadWriteLockPath path = lockPathMap.get(lockId);
        Optional.ofNullable(path).ifPresent(o->{
            Optional.ofNullable(o.getReadWrite().get(false).getPath()).ifPresent(p->{
                for (String el:p
                     ) {
                    readLocks.remove(el);
                }
            });
            Optional.ofNullable(o.getReadWrite().get(true).getPath()).ifPresent(q->{
                for (String el:q
                     ) {
                    writeLocks.remove(el);
                }
            });
        });
        lockPathMap.remove(lockId);
    }

    boolean exists(String lockId) {
        return lockPathMap.containsKey(lockId);
    }


    Set<String> getLocks() {
        Set<String> set = writeLocks.keySet().stream().map(o -> String.format("%s-write", o)).collect(Collectors.toSet());
        set.addAll(readLocks.keySet().stream().map(o->String.format("%s-read", o)).collect(Collectors.toSet()));
        return set;
    }

    private void filterReadLocks(ReadWriteLockPath.LockPath path, String lockId) {
        List<String> locks = path.getPath();
        List<String> filter = new ArrayList<>();
        if(null == locks) {
            return;
        }
        for (String lock:locks
        ) {
            if(writeLocks.containsKey(lock) || readLocks.containsKey(lock))  {
                continue;
            }
            filter.add(lock);
            writeLocks.put(lock, lockId);
        }
        path.setPath(filter);
    }

    private void filterWriteLocks(TransactionLevelEx level, ReadWriteLockPath.LockPath path, String lockId) throws Exception {
        List<String> locks = path.getPath();
        List<String> filter = new ArrayList<>();
        if(null == locks) {
            return;
        }
        for (String lock:locks
        ) {
            if(writeLocks.containsKey(lock)) {
                continue;
            }
            if(readLocks.containsKey(lock)) {
                GlobalLockManager.INSTANCE().release(level, Collections.singletonList(lock), null);
                String readLockId = readLocks.get(lock);
                lockPathMap.get(readLockId).getReadWrite().get(false).getPath().remove(lock);
                readLocks.remove(lock);
            }
            filter.add(lock);
            writeLocks.put(lock, lockId);
        }
        path.setPath(filter);
    }

}
