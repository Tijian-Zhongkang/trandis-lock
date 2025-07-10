package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.IRedisLocker;
import com.xm.sanvanfo.common.CallBack;
import com.xm.sanvanfo.common.Disposable;
import com.xm.sanvanfo.common.utils.LruThreadSafeGetter;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.scriptor.ResetPathScript;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Function;


@Slf4j
class LockRepairManager implements LockPathSplitter, Disposable {

    private final static Integer repairWait = 100;

    @Override
    public void dispose() {
        lockMap.dispose();
    }

    private static class RepairDelayed implements Delayed {

        @Getter
        private final String path;
        private long expire;
        private final long start = Clock.systemDefaultZone().millis();
        RepairDelayed(String path, long expire) {
            this.path = path;
            this.expire = expire;
        }

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            return start + expire - Clock.systemDefaultZone().millis();
        }

        @Override
        public int compareTo(Delayed o) {
            return (int)(o.getDelay(TimeUnit.NANOSECONDS) - getDelay(TimeUnit.NANOSECONDS));
        }

    }

    private final String id;
    private final CoordinatorConfig config;
    private final IRedisLocker locker;
    private final Leader leader;
    private final ResetPathScript resetPathScript;
    private final ExecutorService executor;
    private final ExecutorService queueExecutor;

    private final Map<String, LockPathInfo> mapLockPath = new ConcurrentHashMap<>();
    private final LruThreadSafeGetter<String, StampedLock> lockMap = new LruThreadSafeGetter<>(3600L * 24, 30L, StampedLock::new);
    private final DelayQueue<RepairDelayed> delayeds = new DelayQueue<>();

    private volatile boolean shutdown = false;


    LockRepairManager(String id, CoordinatorConfig config, IRedisLocker locker, Leader leader) {
        this.locker = locker;
        this.leader = leader;
        this.id = id;
        this.config = config;
        resetPathScript = new ResetPathScript();
        executor = Executors.newFixedThreadPool(config.getRepairThreadNum(), new DefaultThreadFactory("LockRepairManager-Repair"));
        queueExecutor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("LockRepairManager-Queue"));
        LockRepairManager manager = this;
        queueExecutor.execute(() ->{
            while (!shutdown && !Thread.currentThread().isInterrupted()) {

                RepairDelayed delayed = delayeds.poll();
                synchronized (manager) {
                    if (null == delayed) {
                        try {
                            manager.wait(delayeds.isEmpty() ? config.getIdleWaitMills() : repairWait);
                        }
                        catch (InterruptedException ignore) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if(null != delayed) {
                    processRepair(delayed);
                }

            }
        });

    }



    boolean acquireLock(AcquireResponse response) {
        ObjectHolder<FollowerInfo> objectHolder = new ObjectHolder<>();
        if(nodeIsNotExist(response.getDestCoordinatorId(), objectHolder)) {
            log.warn("skip acquire lock follower is deleted by leader follower id:{}, response:{}", response.getDestCoordinatorId(), response.toString());
            return false;
        }
        try {
            if (null != objectHolder.getObj()) {
               objectHolder.getObj().getLock().lock();
            }
            boolean needRepair = false;
            if(null != response.getLockPath()) {
                for (NodeLockInfo.LockInfo lockInfo : response.getLockPath()
                ) {
                    LockSplit parts = splitNotifyLockPart(lockInfo.getPath());
                    ThreadLockInfo info = create(lockInfo, response.getDestCoordinatorId(), parts.getReadWrite(), response.getThreadId());
                    boolean repair = checkAcquireSuccess(parts.getPath(), info);
                    if (repair) {
                        repair(parts.getPath());
                        needRepair = true;
                    } else {
                        addPathLock(parts.getPath(), info);
                        if (null != objectHolder.getObj()) {
                            objectHolder.getObj().getNodeLockInfo().getLocks().put(lockInfo.getPath(), lockInfo);
                            objectHolder.getObj().getNodeLockInfo().getLockWait().remove(lockInfo.getPath());
                        }
                    }
                }
            }

            NodeLockInfo.LockWaitInfo wait = response.getWaitPath();
            if(null != wait) {
                LockSplit parts = splitNotifyLockPart(wait.getPath());
                ThreadLockWaitInfo waitInfo = create(response.getDestCoordinatorId(), parts.getReadWrite(), response.getThreadId());
                boolean repair = checkAcquireWait(parts.getPath(), waitInfo);
                if (repair) {
                    repair(parts.getPath());
                    needRepair = true;
                } else {
                    addPathWait(parts.getPath(), waitInfo);
                    if (null != objectHolder.getObj()) {
                        objectHolder.getObj().getNodeLockInfo().getLockWait().put(wait.getPath(), wait);
                    }
                }
            }
            return needRepair;
        }
        finally {
            if (null != objectHolder.getObj()) {
                objectHolder.getObj().getLock().unlock();
            }
        }
    }

    boolean releaseLock(ReleaseResponse response, List notifiedList) {
        ObjectHolder<FollowerInfo> objectHolder = new ObjectHolder<>();
        if(nodeIsNotExist(response.getDestCoordinatorId(), objectHolder)) {
            log.warn("skip release lock follower is deleted by leader follower id:{}, response:{}", response.getDestCoordinatorId(), response.toString());
            return false;
        }
        try {
            if (null != objectHolder.getObj()) {
                objectHolder.getObj().getLock().lock();
            }
            Map<String, List<LockSplit>> mapNotify = convertToNotifyMap(notifiedList);
            boolean needRepair = false;
            if (null != response.getReleasePaths()) {
                for (NodeLockInfo.LockInfo lockInfo : response.getReleasePaths()
                ) {
                    LockSplit parts = splitNotifyLockPart(lockInfo.getPath());
                    boolean repair = checkReleaseSuccess(parts.getPath(), response.getDestCoordinatorId(),
                            response.getThreadId(), parts.getReadWrite().equals("read"), lockInfo.getReEnter(), mapNotify);
                    if (repair) {
                        repair(parts.getPath());
                        needRepair = true;
                    } else {
                        delPathLock(parts.getPath(), response.getDestCoordinatorId(), response.getThreadId(), parts.getReadWrite().equals("read"), lockInfo.getReEnter());
                        if (null != objectHolder.getObj()) {
                            objectHolder.getObj().getNodeLockInfo().getLocks().remove(lockInfo.getPath());
                        }
                    }
                }
            }
            if (null != response.getFailPaths()) {
                for (NodeLockInfo.LockInfo lockInfo : response.getFailPaths()) {
                    LockSplit parts = splitNotifyLockPart(lockInfo.getPath());
                    boolean repair = checkReleaseFail(parts.getPath(), response.getDestCoordinatorId(),
                            response.getThreadId(), parts.getReadWrite().equals("read"), lockInfo.getReEnter());
                    if (repair) {
                        repair(parts.getPath());
                        needRepair = true;
                    } else {
                        delPathWait(parts.getPath(), response.getDestCoordinatorId(), response.getThreadId(), parts.getReadWrite().equals("read"));
                        if (null != objectHolder.getObj() && null != objectHolder.getObj().getNodeLockInfo()) {
                            objectHolder.getObj().getNodeLockInfo().getLockWait().remove(lockInfo.getPath());
                        }
                    }
                }
            }
            return needRepair;
        }
        finally {
            if (null != objectHolder.getObj()) {
                objectHolder.getObj().getLock().unlock();
            }
        }
    }

    private Map<String, List<LockSplit>> convertToNotifyMap(List notifiedList) {
        Map<String, List<LockSplit>> map = new HashMap<>();
        for (Object notify:notifiedList) {
            LockSplit parts = splitNotifyLockPart(notify.toString());
            if(!map.containsKey(parts.getPath())) {
                map.put(parts.getPath(), new ArrayList<>());
            }
            map.get(parts.getPath()).add(parts);
        }
        return map;
    }

    boolean checkOneWait(String path, Long result) {

        LockSplit parts = splitNotifyLockPart(path);
        long stamp = lockMap.get(parts.getPath()).readLock();
        try {
            String pathKey = String.format("%s-%s-%s", parts.getCoordinatorId(),
                    parts.getThreadId(), parts.getReadWrite());
            LockPathInfo info = mapLockPath.get(parts.getPath());
            if(null != info && info.isRepairing()) {
                return true;
            }
            if(result.equals(0L) && info != null && info.getLockWaits().containsKey(pathKey)) {
                log.warn("checkOneWait path is {}, result is 0 and info wait is {}", path, info.getLockWaits().toString());
                repair(parts.getPath());
                return true;
            }
            if(result.equals(1L) && info != null && !info.getLockWaits().containsKey(pathKey)) {
                log.warn("checkOneWait path is {}, result is 1 and info wait is {}", path, info.getLockWaits().toString());
                repair(parts.getPath());
                return true;
            }
            if(result.equals(2L) && info != null && !info.getLockWaits().containsKey(pathKey) && !info.getNotifyInfo().contains(pathKey)) {
                log.warn("checkOneWait path is {}, result is 2 and info wait is {}", path, info.getLockWaits().toString());
                repair(parts.getPath());
                return true;
            }
            return false;
        }
        finally {
            lockMap.get(parts.getPath()).unlockRead(stamp);
        }
    }

    boolean getNextNotified(String path, List notifies) {
        LockSplit parts = splitNotifyLockPart(path);
        long stamp = lockMap.get(parts.getPath()).readLock();
        try {
            LockPathInfo info = mapLockPath.get(parts.getPath());
            String originPath = String.format("%s-%s-%s", parts.getCoordinatorId(),
                    parts.getThreadId(), parts.getReadWrite());
            if(info != null) {
                info.getNotifyInfo().remove(originPath);
            }
            for (Object o:notifies
                 ) {
                LockSplit notifyParts = splitNotifyLockPart(o.toString());
                String notifyPath = String.format("%s-%s-%s", notifyParts.getCoordinatorId(),
                        notifyParts.getThreadId(), notifyParts.getReadWrite());
                if(null!= info && !info.getLockWaits().containsKey(notifyPath)) {
                    log.warn("getNextNotified path is {}, notifyPath is {} info is {}", path, notifyPath, info.toString());
                    repair(parts.getPath());
                    return true;
                }
            }
            return false;
        }
        finally {
            lockMap.get(parts.getPath()).unlockRead(stamp);
        }
    }

    List<String> notifyLock(List notifies) {
        List<String> filterList = new ArrayList<>();
        for (Object str:notifies
             ) {
            LockSplit parts = splitNotifyLockPart(str.toString());
            boolean delWait = false;
            long stamp = lockMap.get(parts.getPath()).readLock();
            try {
                LockPathInfo info = mapLockPath.computeIfAbsent(parts.getPath(), o->new LockPathInfo());
                String key = String.format("%s-%s-%s", parts.getCoordinatorId(), parts.getThreadId(), parts.getReadWrite());
                if (!info.isRepairing() && !info.getLocks().containsKey(key)) {
                    log.debug("----------------add notify:{}", key);
                    boolean ret = info.getNotifyInfo().add(key);
                    if(ret) {
                        filterList.add(str.toString());
                    }
                    if(info.getLockWaits().containsKey(key)) {
                        delWait = true;
                    }
                } else {
                    if(!info.isRepairing()) {
                        log.warn("notifyLock path is {}, info is {}", str.toString(), info.toString());
                        repair(parts.getPath());
                    }
                }
            }
            catch (Exception ex) {
                log.error("notifyLock ex: {}", BusinessException.exceptionFullMessage(ex));
            }
            finally {
                lockMap.get(parts.getPath()).unlockRead(stamp);
            }
            if(delWait) {
                delPathWait(parts.getPath(), parts.getCoordinatorId(), parts.getThreadId(), parts.getReadWrite().equals("read"), false);
            }
        }
        return filterList;
    }

    void reBuildLeaderLock(NodeLockInfo lockInfo) {
        reBuildPathLock(id, lockInfo.deepCopy());
    }

    boolean reBuildLock(String followerId, NodeLockInfo nodeLockInfo) {
        FollowerInfo followerInfo = leader.getFollowersMap().get(followerId);
        if(null == followerInfo) {
            log.warn("follower is deleted by lead in rebuilding lock, follower id:{}, node lock info:{}", followerId, nodeLockInfo.toString());
            return true;
        }
        try {
            followerInfo.getLock().lock();
            deletePathLock(followerInfo.getNodeLockInfo());
            NodeLockInfo copy = nodeLockInfo.deepCopy();
            boolean ret = reBuildPathLock(followerId, copy);
            followerInfo.setNodeLockInfo(copy);
            return ret;
        }
        finally {
            followerInfo.getLock().unlock();
        }

    }

    private void deletePathLock(NodeLockInfo nodeLockInfo) {
        if(null == nodeLockInfo) {
            return;
        }
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, NodeLockInfo.LockInfo> entry : nodeLockInfo.getLocks().entrySet()
        ) {
            LockSplit parts = splitNotifyLockPart(entry.getKey());
            delPathLock(parts.getPath(), parts.getCoordinatorId(), parts.getThreadId(), parts.getReadWrite().equals("read"), entry.getValue().getReEnter());
            remove.add(entry.getKey());
        }
        for (String removeKey:remove
             ) {
            nodeLockInfo.getLocks().remove(removeKey);
        }

        List<String> removeWait = new ArrayList<>();
        for (Map.Entry<String, NodeLockInfo.LockWaitInfo> entry : nodeLockInfo.getLockWait().entrySet()
        ) {
            LockSplit parts = splitNotifyLockPart(entry.getKey());
            delPathWait(parts.getPath(), parts.getCoordinatorId(), parts.getThreadId(), parts.getReadWrite().equals("read"));
            removeWait.add(entry.getKey());
        }
        for (String removeWaitKey:removeWait
             ) {
            nodeLockInfo.getLockWait().remove(removeWaitKey);
        }
    }


    boolean deleteNode(String followerId, List locks, List waits) {
        FollowerInfo followerInfo = leader.getFollowersMap().get(followerId);
        if(null == followerInfo) {
            log.warn("follower is deleted by lead in rebuilding lock, follower id:{}, node locks:{}, waits:{}", followerId,
                    convertToString(locks), convertToString(waits));
            return true;
        }
        try {
            followerInfo.getLock().lock();
            ObjectHolder<Boolean> needRepair = new ObjectHolder<>(false);

            Map<String, NodeLockInfo.LockInfo> lockInfoMap = null != followerInfo.getNodeLockInfo() && null != followerInfo.getNodeLockInfo().getLocks() ?
                    followerInfo.getNodeLockInfo().getLocks() : new HashMap<>();

            deleteNodeLockMapCheck(lockInfoMap, needRepair, locks,
                    t -> delPathLock(t.getPath(), t.getCoordinatorId(), t.getThreadId(), t.getReadWrite().equals("read"), -1));
            Map<String, NodeLockInfo.LockWaitInfo> lockWaitInfoMap = null != followerInfo.getNodeLockInfo() && null != followerInfo.getNodeLockInfo().getLockWait() ?
                    followerInfo.getNodeLockInfo().getLockWait() : new HashMap<>();
            deleteNodeLockMapCheck(lockWaitInfoMap, needRepair, waits, t -> delPathWait(t.getPath(), t.getCoordinatorId(), t.getThreadId(), t.getReadWrite().equals("read")));

            return needRepair.getObj();
        }
        finally {
            followerInfo.getLock().unlock();
        }
    }

    private boolean reBuildPathLock(String id, NodeLockInfo nodeLockInfo) {
        List<String> remove = new ArrayList<>();
        boolean ret = true;
        List<Map.Entry<String, NodeLockInfo.LockInfo>> sortsEntries = new ArrayList<>(nodeLockInfo.getLocks().entrySet());
        sortsEntries.sort((o1,o2)->{
            LockSplit parts1 = splitNotifyLockPart(o1.getKey());
            LockSplit parts2 = splitNotifyLockPart(o2.getKey());
            if(String.format("%s-%s", parts1.getCoordinatorId(), parts1.getThreadId()).equals(
                String.format("%s-%s", parts2.getCoordinatorId(), parts2.getThreadId()))) {
                if(parts1.getReadWrite().equals("read") && !parts2.getReadWrite().equals("read")) {
                    return 1;
                }
                else if(parts2.getReadWrite().equals("read") && !parts1.getReadWrite().equals("read")) {
                    return -1;
                }
                else {
                    return 0;
                    }
            }
            return String.format("%s-%s", parts2.getCoordinatorId(), parts2.getThreadId()).compareTo(
                    String.format("%s-%s", parts1.getCoordinatorId(), parts1.getThreadId()));
        });
        for (Map.Entry<String, NodeLockInfo.LockInfo> entry : sortsEntries
        ) {
            LockSplit parts = splitNotifyLockPart(entry.getKey());
            ThreadLockInfo info = create(entry.getValue(), id, parts.getReadWrite(), parts.getThreadId());
            //if locked by other skip the lock
            if(!isRepairing(parts.getPath()) && !checkAcquireSuccessWithCheck(parts.getPath(), info, null)) {
                addPathLock(parts.getPath(), info);
            }
            else {
                remove.add(entry.getKey());
                if(isRepairing(parts.getPath())) {
                    ret = false;
                }
            }
        }
        for (String removeKey:remove
        ) {
            nodeLockInfo.getLocks().remove(removeKey);
        }
        List<String> removeWait = new ArrayList<>();
        for (Map.Entry<String, NodeLockInfo.LockWaitInfo> entry : nodeLockInfo.getLockWait().entrySet()
        ) {
            LockSplit parts = splitNotifyLockPart(entry.getKey());
            ThreadLockWaitInfo info = create(id, parts.getReadWrite(), parts.getThreadId());
            if(isRepairing(parts.getPath())) {
                removeWait.add(entry.getKey());
                ret = false;
            }
            else {
                addPathWait(parts.getPath(), info);
            }

        }
        for (String removeWaitKey:removeWait
             ) {
            nodeLockInfo.getLockWait().remove(removeWaitKey);
        }
        return ret;
    }

    private void deleteNodeLockMapCheck(Map<String, ?> map, ObjectHolder<Boolean> needRepair, List paths, Consumer<LockSplit> consumer) {

        for (Object o:paths
        ) {
            String path = o.toString();
            LockSplit parts = splitNotifyLockPart(path);
            if(!map.containsKey(path)) {
                log.warn("deleteNodeLockMapCheck path is {}, map is {}", parts.getPath(), map.toString());
                repair(parts.getPath());
            }
            else {
                if(isRepairing(parts.getPath())) {
                   log.debug("{} is repairing", parts.getPath());
                   continue;
                }
                map.remove(path);
                consumer.accept(parts);
            }
        }
        for (String str:map.keySet()
        ) {
            LockSplit parts = splitNotifyLockPart(str);
            log.warn("deleteNodeLockMapCheck path is {}, map's remain is {}", parts.getPath(), map.toString());
            repair(parts.getPath());
            needRepair.setObj(true);
        }
    }

    boolean isRepairing(AcquireRequest body) {
        ObjectHolder<Boolean> repair = new ObjectHolder<>(false);
        checkRepairing(Optional.ofNullable(body.getReadPath()).map(ReadWriteLockPath.LockPath::getPath).orElse(null), repair);
        if(repair.getObj()) {
            return true;
        }
        checkRepairing(Optional.ofNullable(body.getWritePath()).map(ReadWriteLockPath.LockPath::getPath).orElse(null), repair);
        return repair.getObj();
    }

    boolean isRepairing(ReleaseRequest body) {
        ObjectHolder<Boolean> repair = new ObjectHolder<>(false);
        checkRepairing(body.getReadPath(), repair);
        if(repair.getObj()) {
            return true;
        }
        checkRepairing(body.getWritePath(), repair);
       return repair.getObj();
    }

    boolean isRepairing(String path) {
        ObjectHolder<Boolean> repair = new ObjectHolder<>(false);
        checkRepairing(Collections.singletonList(path), repair);
        return repair.getObj();
    }

    boolean isRepairing(NodeLockInfo nodeLockInfo) {
        ObjectHolder<Boolean> repair = new ObjectHolder<>(false);
        Map<String, NodeLockInfo.LockInfo> lockInfoMap = null != nodeLockInfo && null != nodeLockInfo.getLocks() ?
                nodeLockInfo.getLocks() : new HashMap<>();
        checkRepairing(new ArrayList<>(lockInfoMap.keySet()), repair);
        if(repair.getObj()) {
            return true;
        }
        Map<String, NodeLockInfo.LockWaitInfo> lockWaitInfoMap = null != nodeLockInfo && null != nodeLockInfo.getLockWait() ?
                nodeLockInfo.getLockWait() : new HashMap<>();

        checkRepairing(new ArrayList<>(lockWaitInfoMap.keySet()), repair);
        return repair.getObj();
    }

    void shutdown() {
        this.shutdown = true;
        lockMap.dispose();
        executor.shutdown();
        queueExecutor.shutdown();
    }

    private ThreadLockInfo create(NodeLockInfo.LockInfo lockInfo, String coordinateId, String read, String threadId) {
        ThreadLockInfo info = new ThreadLockInfo();
        info.setCoordinatorId(coordinateId);
        info.setReadWrite(read.equals("read"));
        info.setThreadId(threadId);
        info.setReEnterTimes(lockInfo.getReEnter());
        return info;
    }

    private ThreadLockWaitInfo create(String coordinateId, String read, String threadId) {
        ThreadLockWaitInfo waitInfo = new ThreadLockWaitInfo();
        waitInfo.setCoordinatorId(coordinateId);
        waitInfo.setReadWrite(read.equals("read"));
        waitInfo.setThreadId(threadId);
        waitInfo.setAddTime(Clock.systemDefaultZone().millis());
        return waitInfo;
    }

    private boolean nodeIsNotExist(String nodeId, ObjectHolder<FollowerInfo> objectHolder) {
        if(nodeId.equals(id)) {
            return false;
        }
        FollowerInfo followerInfo = leader.getFollowersMap().get(nodeId);
        objectHolder.setObj(followerInfo);
        return null == followerInfo;
    }

    private boolean checkAcquireWait(String part, ThreadLockWaitInfo waitInfo) {

        long stamp = lockMap.get(part).readLock();
        try {
            if(leader.isNotInit(waitInfo.getCoordinatorId())) {
                return false;
            }
            LockPathInfo info = mapLockPath.get(part);
            if(null  == info) {
                log.warn("checkAcquireWait {} is null", part);
                return true;
            }
            if(info.isRepairing()) {
                log.warn("checkAcquireWait {} is repairing", part);
                return true;
            }
            String fullPath = String.format("%s-%s-%s", waitInfo.getCoordinatorId(),
                    waitInfo.getThreadId(), waitInfo.getReadWrite() ? "read" : "write");
            boolean ret =  info.getLocks().containsKey(fullPath) ||
                    (info.getLockWaits().size() == 0 && (info.getNotifyInfo().contains(fullPath) || info.getNotifyInfo().size() == 0)
                            && canAcquire(info.getLocks(), waitInfo.getCoordinatorId(), waitInfo.getThreadId(), waitInfo.getReadWrite()));
            if(ret) {
                log.warn("checkAcquireWait key {} return true full path is {}, info is {}", part, fullPath, info.toString());
            }
            return ret;
        }
        finally {
            lockMap.get(part).unlockRead(stamp);
        }
    }
    

    private boolean checkAcquireSuccess(String part, ThreadLockInfo lock) {
        return checkAcquireSuccessWithCheck(part, lock, l->leader.isNotInit(l.getCoordinatorId()));
    }

    private boolean checkAcquireSuccessWithCheck(String part, ThreadLockInfo lock, Function<ThreadLockInfo, Boolean> func) {
        long stamp = lockMap.get(part).readLock();
        try {

            if(null != func && func.apply(lock)) {
                return false;
            }
            LockPathInfo info = mapLockPath.get(part);
            if(null  == info) {
                return false;
            }
            if(info.isRepairing()) {
                log.warn("checkAcquireSuccessWithCheck {} is in repairing", part);
                return true;
            }
            String fullPath = String.format("%s-%s-%s", lock.getCoordinatorId(),
                    lock.getThreadId(), lock.getReadWrite() ? "read" : "write");
            boolean ret =  info.getLockWaits().containsKey(fullPath) ||
                    (info.getLocks().containsKey(fullPath) && !info.getLocks().get(fullPath).getReEnterTimes().equals(-1) && ! lock.getReEnterTimes().equals(-1)
                            && info.getLocks().get(fullPath).getReEnterTimes() > lock.getReEnterTimes() ||
                            (!info.getLocks().containsKey(fullPath) && !canAcquire(info.getLocks(), lock.getCoordinatorId(), lock.getThreadId(), lock.getReadWrite())));
            if(ret) {
                log.warn("checkAcquireSuccessWithCheck key {} return true full path is {}, info is {}", part, fullPath, info.toString());
            }
            return ret;
        }
        finally {
            lockMap.get(part).unlockRead(stamp);
        }
    }

    private boolean checkReleaseSuccess(String part, String coordinatorId, String threadId, Boolean read, Integer reEnter, Map<String, List<LockSplit>> mapNotify) {
        long stamp = lockMap.get(part).readLock();
        try {
            if(leader.isNotInit(coordinatorId)) {
                return false;
            }
            LockPathInfo info = mapLockPath.get(part);
            if(null  == info) {
                log.warn("checkReleaseSuccess {} is null", part);
                return true;
            }
            if(info.isRepairing()) {
                log.warn("checkReleaseSuccess {} is repairing", part);
                return true;
            }
            String fullPath = String.format("%s-%s-%s", coordinatorId,
                    threadId, read ? "read" : "write");
            ThreadLockInfo lockInfo = info.getLocks().get(fullPath);
            boolean ret =  null == lockInfo || (lockInfo.getReEnterTimes() < reEnter && reEnter > 0) ||
                    (info.getLocks().size() == 1 && info.getLockWaits().size() > 0 && !mapNotify.containsKey(part));
            if(ret) {
                log.warn("checkReleaseSuccess key {} return true full path is {}, info is {}", part, fullPath, info.toString());
            }

            return ret;
        }
        finally {
            lockMap.get(part).unlockRead(stamp);
        }
    }

    private boolean checkReleaseFail(String part, String coordinatorId, String threadId, Boolean read, Integer reEnter) {
        long stamp = lockMap.get(part).readLock();
        try {
            if(leader.isNotInit(coordinatorId)) {
                return false;
            }
            LockPathInfo info = mapLockPath.get(part);
            if(null  == info) {
                return false;
            }
            if(info.isRepairing()) {
                log.warn("checkReleaseFail {} is repairing", part);
                return true;
            }
            String fullPath = String.format("%s-%s-%s", coordinatorId,
                    threadId, read ? "read" : "write");
            ThreadLockInfo lockInfo = info.getLocks().get(fullPath);
            if(null == lockInfo) {
                return false;
            }
            boolean ret =  lockInfo.getReEnterTimes() <= reEnter || reEnter <= 0;
            if(ret) {
                log.warn("checkReleaseFail key is {} return true full path is {}, info is {}", part, fullPath, info.toString());
            }
            return ret;
        }
        finally {
            lockMap.get(part).unlockRead(stamp);
        }
    }

    private boolean canAcquire(Map<String, ThreadLockInfo> info, String coordinatorId, String threadId, Boolean read) {
        if(info.size() == 0) {
            return true;
        }
        if(read) {
            String write = String.format("%s-%s-write", coordinatorId, threadId);
            if(info.containsKey(write)) {
                return true;
            }
            return info.values().stream().allMatch(ThreadLockInfo::getReadWrite);
        }
        return false;
    }


    private void addPathLock(String path, ThreadLockInfo threadLockInfo) {
        long stamp = lockMap.get(path).writeLock();
        try{
            LockPathInfo info = mapLockPath.computeIfAbsent(path, o->new LockPathInfo());
            String key = String.format("%s-%s-%s", threadLockInfo.getCoordinatorId(),
                    threadLockInfo.getThreadId(), threadLockInfo.getReadWrite() ? "read" : "write");

            info.getLocks().put(key, threadLockInfo);
            boolean ret = info.getNotifyInfo().remove(key);
            if(ret) {
                log.debug("------------------- lock remove notify:{}", key);
            }
        }
        finally {
            lockMap.get(path).unlockWrite(stamp);
        }
    }

    private void addPathWait(String path, ThreadLockWaitInfo threadLockWaitInfo) {
        long stamp = lockMap.get(path).writeLock();
        try{
            LockPathInfo info = mapLockPath.computeIfAbsent(path, o->new LockPathInfo());
            String key = String.format("%s-%s-%s", threadLockWaitInfo.getCoordinatorId(),
                    threadLockWaitInfo.getThreadId(), threadLockWaitInfo.getReadWrite() ? "read" : "write");
            info.getLockWaits().put(key
                    , threadLockWaitInfo);
            boolean ret = info.getNotifyInfo().remove(key);
            if(ret) {
                log.debug("-------------------wait remove notify:{}", key);
            }
        }
        finally {
            lockMap.get(path).unlockWrite(stamp);
        }
    }

    private void delPathLock(String path, String coordinatorId, String threadId, Boolean readWrite, Integer reEnter) {
        long stamp = lockMap.get(path).writeLock();
        try{
            LockPathInfo info = mapLockPath.get(path);
            if(null == info) {
                log.warn("{} does not in the map", path);
                return;
            }
            String key =  String.format("%s-%s-%s", coordinatorId,
                    threadId, readWrite ? "read" : "write");
            if(reEnter <= 0) {
                info.getLocks().remove(key);
                if(!info.isRepairing() && info.getLockWaits().size() == 0 && info.getLocks().size() == 0 && info.getNotifyInfo().size() == 0) {
                    mapLockPath.remove(path);
                }
            }
            else {
                ThreadLockInfo lockInfo = info.getLocks().get(key);
                if(null != lockInfo) {
                    lockInfo.setReEnterTimes(reEnter);
                }
            }
        }
        finally {
            lockMap.get(path).unlockWrite(stamp);
        }
    }

    private void delPathWait(String path, String coordinatorId, String threadId, Boolean readWrite, Boolean  deleteNotify) {
        long stamp = lockMap.get(path).writeLock();
        try {
            LockPathInfo info = mapLockPath.get(path);
            if (null == info) {
                log.warn("{} does not in the map", path);
                return;
            }
            String key = String.format("%s-%s-%s", coordinatorId,
                    threadId, readWrite ? "read" : "write");
            info.getLockWaits().remove(key);
            if(deleteNotify) {
                boolean ret = info.getNotifyInfo().remove(key);
                if(ret) {
                    log.debug("--------------remove notify:{}", key);
                }
            }
            if(!info.isRepairing() && info.getLockWaits().size() == 0 && info.getLocks().size() == 0 && info.getNotifyInfo().size() == 0) {
                mapLockPath.remove(path);
            }
        }
        finally {
            lockMap.get(path).unlockWrite(stamp);
        }
    }

    private void delPathWait(String path, String coordinatorId, String threadId, Boolean readWrite) {
        delPathWait(path, coordinatorId, threadId, readWrite, true);
    }

    private void repair(String path) {
        LockPathInfo info = mapLockPath.computeIfAbsent(path, o->new LockPathInfo());
        if(info.isRepairing()) {
            log.debug("{} is repairing", path);
            return;
        }
        info.setRepairing(true);
        delayeds.add(new RepairDelayed(path, 0));
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void processRepair(RepairDelayed delayed) {
        String path = delayed.getPath();
        LockPathInfo info = mapLockPath.get(path);
        if(null == info || !info.isRepairing()) {
            log.warn("info is null or no repairing path:{}", path);
            return;
        }
        LockRepairManager manager = this;
        executor.execute(() -> {
            try {
                ObjectHolder<Boolean> needReElection = new ObjectHolder<>(false);
                List<String> notifies = new ArrayList<>();
                boolean merge = resetLockInfo(path, info, needReElection, notifies);
                if(needReElection.getObj()) {
                    leader.setNeedReElectionTrue();
                    return;
                }
                if (merge) {
                    info.setRepairing(false);
                    if(notifies.size() > 0) {
                        LockParser lockParser = leader.getLockParser();
                        lockParser.notifyWaitKeys(lockParser.filterNotInit(notifies));
                    }
                } else {
                    delayeds.add(new RepairDelayed(path, repairWait));
                    synchronized (manager) {
                        manager.notifyAll();
                    }
                }
            }
            catch (Exception ex) {
                log.error("repair fail:{}, msg:{}", path, BusinessException.exceptionFullMessage(ex));
            }
        });
    }

    private boolean resetLockInfo(String path, LockPathInfo info, ObjectHolder<Boolean> needReElection, List<String> notifies) {
            ObjectHolder<Boolean> ret = new ObjectHolder<>(true);
            Set<String> nodes = getNodeIds(info);
            nodeLockFunc(new ArrayList<>(nodes), () -> {
                long stamp = lockMap.get(path).readLock();
                try {
                    for (String node:nodes
                         ) {
                        if(leader.isNotInit(node) && leader.getFollowersMap().containsKey(node)
                                && leader.getFollowersMap().get(node).getInit().equals(FollowerInfo.InitStatus.NoInitRepairing)) {
                            ret.setObj(false);
                            log.info("repairing wait because node is not init {}", node);
                            break;
                        }
                    }
                    if(ret.getObj()) {
                        //now local info is completed,set local info to redis
                        try {
                            log.warn("reset info key {}, info {}", path, info);
                            List list = locker.execScript(resetPathScript.script(), List.class, null,
                                    resetPathScript.scriptKey(config.getLeaderKey(), path, config.getLockWaitSuffix(), config.getLockListSuffix()), createArgvs(path, info, notifies));
                            Integer result = Integer.parseInt(list.get(0).toString());
                            if(result.equals(-1)) {
                                needReElection.setObj(true);
                            }
                            else {
                                log.info("reset path success,path:{}", path);
                            }
                        }
                        catch (Exception ex) {
                            log.error("reset lock info error,path:{}, msg:{}", path, BusinessException.exceptionFullMessage(ex));
                            ret.setObj(false);
                        }
                    }
                }
                finally {
                    lockMap.get(path).unlockRead(stamp);
                }});
            return ret.getObj();

    }


    @SuppressWarnings("unchecked")
    private List<Object> createArgvs(String path, LockPathInfo info, List notifies) {
        List<Object> list = new ArrayList<>();
        list.add(CoordinatorConst.idStr);
        list.add(id);
        addCommonLockParameter(list, config);

        List<Object> writeLock = new ArrayList<>();
        List<Object> readLock = new ArrayList<>();
        Map<String, ThreadLockInfo> locks = info.getLocks();
        for (Map.Entry<String, ThreadLockInfo> lock:locks.entrySet()
             ) {
            if(lock.getValue().getReadWrite()) {
                readLock.add(lock.getKey());
                readLock.add(lock.getValue().getReEnterTimes());
            }
            else {
                writeLock.add(lock.getKey());
                writeLock.add(lock.getValue().getReEnterTimes());
            }
        }
        List<Object> lockWait = new ArrayList<>();
        Map<String, ThreadLockWaitInfo> waits = info.getLockWaits();
        List<Map.Entry<String, ThreadLockWaitInfo>> listEntry = new ArrayList<>(waits.entrySet());
        listEntry.sort((o1, o2)->o2.getValue().getAddTime().compareTo(o1.getValue().getAddTime()));
        List<Object> notifyWait = new ArrayList<>(info.getNotifyInfo());
        for (Object notify:notifyWait
             ) {
            notifies.add(locker.quote(path, config.getSpace()) + "-" + notify.toString());
        }
        boolean read = true;
        List<String> addNotify = new ArrayList<>();
        for (Map.Entry<String, ThreadLockWaitInfo> entry : listEntry
             ) {
            //If the leader dump, the new leader is unaware of the notification
            if(writeLock.size() == 0 && readLock.size() == 0 && notifyWait.size() == 0 && read) {
                notifies.add(locker.quote(path, config.getSpace()) + "-" + entry.getKey());
                addNotify.add(entry.getKey());
                read = entry.getValue().getReadWrite();
            }
            else {
                lockWait.add(entry.getKey());
            }
        }

        for (String add:addNotify
             ) {
            notifyWait.add(add);
            info.getLockWaits().remove(add);
            info.getNotifyInfo().add(add);
        }
        if(addNotify.size() > 0) {
            log.warn("create reset argvs key {}, code for notify {}", path, String.join(",", addNotify));
        }
        list.add(writeLock.size() / 2);
        list.add(readLock.size() / 2);
        list.add(lockWait.size());
        list.add(notifyWait.size());
        list.addAll(writeLock);
        list.addAll(readLock);
        list.addAll(lockWait);
        list.addAll(notifyWait);
        return list;
    }


    private void nodeLockFunc(ArrayList<String> strings, CallBack o) {

        strings.sort(String::compareTo);
        List<ReentrantLock> locks = new ArrayList<>();
        try {
            for (String str : strings
            ) {
                FollowerInfo info = leader.getFollowersMap().get(str);
                if(null != info) {
                    info.getLock().lock();
                    locks.add(info.getLock());
                }
            }
            o.apply();
        }
        finally {
            for (ReentrantLock lock:locks
                 ) {
                lock.unlock();
            }
        }
    }

    private Set<String> getNodeIds(LockPathInfo pathInfo) {
        Set<String> nodes = new HashSet<>();
        if(null != pathInfo) {
            for (ThreadLockInfo lockInfo : pathInfo.getLocks().values()
            ) {
                nodes.add(lockInfo.getCoordinatorId());
            }
            for (ThreadLockWaitInfo waitInfo:pathInfo.getLockWaits().values()
                 ) {
                nodes.add(waitInfo.getCoordinatorId());
            }
        }
        return nodes;
    }

    private void checkRepairing(List<String> paths, ObjectHolder<Boolean> repair) {
        Optional.ofNullable(paths).ifPresent(o->{
            for (String path:o
            ) {
                LockPathInfo info = mapLockPath.get(path);
                if (null != info && info.repairing) {
                    repair.setObj(true);
                    break;
                }
            }
        });
    }

    @Data
    private static class ThreadLockWaitInfo {
        private String coordinatorId;
        private String threadId;
        private Boolean readWrite;
        private Long addTime;
    }


    @Data
    private static class ThreadLockInfo {
        private String coordinatorId;
        private String threadId;
        private Boolean readWrite;
        private Integer reEnterTimes;
    }

    @Data
    private static class LockPathInfo {
        private Map<String, ThreadLockWaitInfo> lockWaits = new ConcurrentHashMap<>();
        private Map<String, ThreadLockInfo> locks = new ConcurrentHashMap<>();
        private Set<String> notifyInfo = new ConcurrentSkipListSet<>();
        private volatile boolean repairing;
    }


}
