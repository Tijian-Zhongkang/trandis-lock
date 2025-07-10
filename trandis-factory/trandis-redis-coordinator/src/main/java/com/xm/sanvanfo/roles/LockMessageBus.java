package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
public class LockMessageBus {

    @AllArgsConstructor
    @Data
    private static class WaitPath {
        private boolean wait;
        private Object waitObj;
        private String waitPath;
    }

    private static LockMessageBus instance = new LockMessageBus();

    private final Map<String, WaitPath> threadLockMap = new ConcurrentHashMap<>();

    private final NodeLockInfo lockInfo = new NodeLockInfo(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());

    private CoordinatorConfig config;

     public static LockMessageBus INSTANCE() {
         return instance;
     }

     public void setLockCoordinatorInfo(CoordinatorConfig config) {
         this.config = config;
     }

     public void acquireLock(List<NodeLockInfo.LockInfo> lockInfos, List<NodeLockInfo.LockWaitInfo> lockWaits) {
         if(null != lockInfos) {
             for (NodeLockInfo.LockInfo lockInfo : lockInfos
             ) {
                 String path = lockInfo.getPath();
                 NodeLockInfo.LockInfo originPath = this.lockInfo.getLocks().get(path);
                 if (null == originPath || originPath.getReEnter() < lockInfo.getReEnter()) {
                     this.lockInfo.getLocks().put(path, lockInfo);
                 }
                 this.lockInfo.getLockWait().remove(path);
             }
         }
         if(null != lockWaits) {
             for (NodeLockInfo.LockWaitInfo lockWait : lockWaits
             ) {
                 this.lockInfo.getLockWait().put(lockWait.getPath(), lockWait);
             }
         }
     }

     public void releaseLock(List<NodeLockInfo.LockInfo> lockInfos) {
         if(null != lockInfos) {
             for (NodeLockInfo.LockInfo lockInfo : lockInfos
             ) {
                 String path = lockInfo.getPath();
                 NodeLockInfo.LockInfo originPath = this.lockInfo.getLocks().get(path);
                 if (null != originPath) {
                     if (lockInfo.getReEnter() <= 0) {
                         this.lockInfo.getLocks().remove(path);
                     } else {
                         this.lockInfo.getLocks().put(path, originPath);
                     }
                 } else {
                     this.lockInfo.getLockWait().remove(path);
                 }
             }
         }

     }

     public NodeLockInfo getLockInfo() {
         return new NodeLockInfo(Collections.unmodifiableMap(this.lockInfo.getLocks()), Collections.unmodifiableMap(this.lockInfo.getLockWait()));
     }

     public boolean wait(String id, String waitPath, long time, TimeUnit unit, long startMills) throws InterruptedException {
         long currentMills = System.currentTimeMillis();
         long waitTime = unit != null && time > 0 ? unit.toMillis(time) : config.getLockMaxWaiMills();
         long nextWaitTime = startMills + waitTime - currentMills;
         nextWaitTime = Math.max(10L, nextWaitTime);
         synchronized (getThreadWaitObj(id).getWaitObj()) {
             getThreadWaitObj(id).setWait(true);
             getThreadWaitObj(id).setWaitPath(waitPath);
             if(config.getLockMaxWaiMills().equals(-1L) && time <= 0) {
                 getThreadWaitObj(id).getWaitObj().wait();
                 getThreadWaitObj(id).setWait(false);
                 getThreadWaitObj(id).setWaitPath("");
                 return true;
             }
             else {
                 getThreadWaitObj(id).getWaitObj().wait(nextWaitTime);
                 currentMills = System.currentTimeMillis();
                 getThreadWaitObj(id).setWait(false);
                 getThreadWaitObj(id).setWaitPath("");
                 return currentMills - startMills < waitTime;
             }
         }
     }

     public boolean notifyAll(String id) {
         synchronized (getThreadWaitObj(id).getWaitObj()) {
             if(getThreadWaitObj(id).isWait()) {
                 getThreadWaitObj(id).getWaitObj().notifyAll();
                 return true;
             }
             return false;
         }
     }

     public List<String> getAllWaitPath() {
         Map<String, WaitPath> map =  Collections.unmodifiableMap(threadLockMap);
         return map.values().stream().filter(WaitPath::isWait).map(WaitPath::getWaitPath).collect(Collectors.toList());
     }

     public void notifyAllThread() {
         threadLockMap.keySet().forEach(o-> {
             synchronized (getThreadWaitObj(o)) {
                 getThreadWaitObj(o).notifyAll();
             }
         });
     }

    private WaitPath getThreadWaitObj(String id) {
        return threadLockMap.computeIfAbsent(id, o->new WaitPath(false, new Object(), ""));
    }
}
