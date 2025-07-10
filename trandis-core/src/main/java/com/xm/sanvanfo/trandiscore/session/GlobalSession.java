package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchLockResourceRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchLockResourceResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.reverse;

@SuppressWarnings({"unused"})
@Slf4j
@Data
public class GlobalSession implements Serializable {
    private static final long serialVersionUID = -8340720208534816766L;
    private GlobalTransactionInfo transactionInfo;
    private ConcurrentHashMap<Integer, BranchSection> sections = new ConcurrentHashMap<>();
    private AtomicInteger atom = new AtomicInteger(0);
    private BranchSection lastSection;
    private ConcurrentLinkedQueue<ReadWriteLockPath> lockLinkedQueue = new ConcurrentLinkedQueue<>();
    private List<ReadWriteLockPath> releaseLinkQueue = Collections.synchronizedList(new ArrayList<>());

    private transient SessionStore sessionStore;
    private transient ExecutorService executorService;
    private transient volatile boolean release;
    private  TransactionLevelEx levelEx;
    private transient AbstractNettyRemotingServer server;
    private transient final SessionLockManager sessionLockManager = new SessionLockManager();
    private transient final Object waitObj = new Object();

    GlobalSession(GlobalTransactionInfo transactionInfo, SessionStore sessionStore, ExecutorService executorService, TransactionLevelEx levelEx,
                  AbstractNettyRemotingServer server) {
        this.transactionInfo = transactionInfo;
        this.sessionStore = sessionStore;
        this.executorService = executorService;
        this.levelEx = levelEx;
        this.server = server;
        this.release = false;
    }

    void initLock() {
        executorService.submit(new LockRunnable());
    }


    public void lockResources(ReadWriteLockPath lockKeys) {
        if(lockKeys.getLockType().equals(BranchLockResourceRequest.LockType.Lock) && sessionLockManager.exists(lockKeys.getLockId())) {
            return;
        }
        if((lockKeys.getLockType().equals(BranchLockResourceRequest.LockType.Release) ||
                lockKeys.getLockType().equals(BranchLockResourceRequest.LockType.ReleaseRead))
                && !sessionLockManager.exists(lockKeys.getLockId())) {
            return;
        }
        lockLinkedQueue.offer(lockKeys);
        synchronized (waitObj) {
            waitObj.notifyAll();
        }
    }

    public int add(BranchSection section) {
        log.debug(String.format("------xid :%s add section %s", transactionInfo.getXid(), section.toString()));
        if(null == section.getSectionNumber()) {
            section.setSectionNumber(atom.incrementAndGet());
        }
        else {
            if(atom.get() <= section.getSectionNumber()) {
                atom.set(section.getSectionNumber());
            }
        }
        if(null == lastSection) {
            section.setCommitLevel(0);
        }
        else {
            if(!lastSection.getEnd()) {
                section.setCommitLevel(lastSection.getCommitLevel() + 1);
            }
            else {
                section.setCommitLevel(lastSection.getCommitLevel());
            }
        }
        sections.put(section.getSectionNumber(), section);
        store();
        lastSection = section;
        return section.getSectionNumber();
    }

    public List<NettyPoolKey> getCommitClients() {
        List<BranchSection> list = new ArrayList<>(sections.values());
        list.sort((t1,t2)-> t2.getSectionNumber() - t1.getSectionNumber());

        List<NettyPoolKey> listKeys = new ArrayList<>();
        HashSet<String> temp = new HashSet<>();
        for (BranchSection section:list
             ) {
            if(!temp.contains(section.getClientKey().getApplicationId())) {
                temp.add(section.getClientKey().getApplicationId());
                listKeys.add(section.getClientKey());
            }
        }
        temp.clear();
        return listKeys;
    }

    public BranchSection setSectionEnd(String xid, NettyPoolKey key, Integer sectionNumber) {
        log.debug(String.format("------xid :%s set section end %d", transactionInfo.getXid(), sectionNumber));
         if(!sections.containsKey(sectionNumber)) {
             BranchSection section = new BranchSection();
             section.setXid(xid);
             section.setClientKey(key);
             section.setSectionNumber(sectionNumber);
             add(section);
         }
         BranchSection section = sections.get(sectionNumber);
         section.setEnd(true);
        store();
        return lastUnclosedSection();
    }

    void clean() {
        log.debug(String.format("-------xid:%s clear", transactionInfo.getXid()));
        sections.clear();
        release = true;
        synchronized (waitObj) {
            waitObj.notifyAll();
        }
        atom.set(0);
        lastSection = null;
    }

    public BranchSection get(Integer number) {
        return sections.get(number);
    }

    public List<BranchSection> getAll() {
        List<BranchSection> collection = new ArrayList<>(sections.values());
        collection.sort(Comparator.comparing(BranchSection::getSectionNumber));
        collection.sort(Comparator.comparing(BranchSection::getSectionNumber));
        return Collections.unmodifiableList(collection);
    }

    private BranchSection lastUnclosedSection() {
        List<BranchSection> collection = getAll();
        for (BranchSection branchSection:reverse(collection)
             ) {
            if(!branchSection.getEnd()) {
                return branchSection;
            }
        }
        return null;
    }

    public Boolean isTrunk(String applicationId) {
        if(!sections.containsKey(1)) {
            return false;
        }
        NettyPoolKey key = sections.get(1).getClientKey();
        return key.getApplicationId().equals(applicationId);
    }

    public Set<String> getLocks() {
        return sessionLockManager.getLocks();
    }

    private void store() {
        if(this.sessionStore.sync()) {
            sessionStore.store(this);
        }
        else {
            executorService.execute(()-> sessionStore.store(this));
        }
    }

    private void sendLockResourcesResponse(ReadWriteLockPath readWriteLockPath, Integer code, String msg) {
        Object obj = readWriteLockPath.getRpcMessage().getBody();
        Asserts.isTrue(obj instanceof BranchLockResourceRequest);
        BranchLockResourceRequest request = (BranchLockResourceRequest)obj;
        BranchLockResourceResponse response = new BranchLockResourceResponse();
        response.setMsg(msg);
        response.setCode(code);
        response.setApplicationId(request.getApplicationId());
        response.setXid(request.getXid());
        response.setServerId(server.getServerId());
        response.setClientId(request.getClientId());
        server.sendAsyncResponse(readWriteLockPath.getRpcMessage(), readWriteLockPath.getChannel(), response);
    }

    // The lock of a session must be in a thread, because the read lock may be upgraded to the write lock
    private class LockRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                ReadWriteLockPath locks = lockLinkedQueue.poll();
                if(null != locks) {
                    if(locks.getLockType().equals(BranchLockResourceRequest.LockType.Lock)) {
                        lock(locks);
                    }
                    else if(locks.getLockType().equals(BranchLockResourceRequest.LockType.ReleaseRead)) {
                        releaseRead(locks);

                    }
                    else {
                        release(locks, true);
                        releaseLinkQueue.remove(locks);
                    }
                }
                if(release) {
                    releaseLinkQueue.forEach(o->release(o, false));
                    releaseLinkQueue.clear();
                    lockLinkedQueue.clear();
                    break;
                }
                synchronized (waitObj) {
                    if (lockLinkedQueue.isEmpty() && !release) {
                        try {
                            waitObj.wait();
                        } catch (InterruptedException ignore) {

                        }
                    }
                }
            }
        }

        private void releaseRead(ReadWriteLockPath locks) {
            List<String> read = locks.getReadWrite().get(false).getPath();
            List<String> write = locks.getReadWrite().get(true).getPath();
            try {
                List<String> list =  GlobalLockManager.INSTANCE().releaseRead(levelEx, read, write);
                sessionLockManager.removeReadLocks(locks.getLockId(), list);
                sendLockResourcesResponse(locks, 200, "");
            }
            catch (Exception ex) {
                log.warn(String.format("release read lock error xid:%s,read path:%s, write path:%s,stack:%s", transactionInfo.getXid(),
                        null == read ? "" : String.join(",", read)
                        , null == write ? "" : String.join(",", write), BusinessException.exceptionFullMessage(ex)));
                sendLockResourcesResponse(locks, 500, ex.getMessage());
            }
        }

        private void release(ReadWriteLockPath locks, boolean sendResponse) {
            List<String> read = locks.getReadWrite().get(false).getPath();
            List<String> write = locks.getReadWrite().get(true).getPath();
            try {
                GlobalLockManager.INSTANCE().release(levelEx, read, write);
                if(sendResponse) {
                    sendLockResourcesResponse(locks, 200, "");
                }
                sessionLockManager.removeLocks(locks.getLockId());
            }
            catch (Exception ex) {
                log.warn(String.format("release lock error xid:%s,read path:%s, write path:%s,stack:%s", transactionInfo.getXid(),
                        null == read ? "" : String.join(",", read)
                        , null == write ? "" : String.join(",", write), BusinessException.exceptionFullMessage(ex)));
                if(sendResponse) {
                    sendLockResourcesResponse(locks, 500, ex.getMessage());
                }
            }
        }

        private void lock(ReadWriteLockPath locks) {
            ReadWriteLockPath.LockPath read = locks.getReadWrite().get(false);
            ReadWriteLockPath.LockPath write = locks.getReadWrite().get(true);
            try {
                Long begin = System.currentTimeMillis();
                sessionLockManager.filterLocks(levelEx, locks);
                GlobalLockManager.INSTANCE().acquire(levelEx, read.getPath(), read.getTimeout(), read.getTimeUnit(),
                        write.getPath(), write.getTimeout(), write.getTimeUnit());
                releaseLinkQueue.add(locks);
                sendLockResourcesResponse(locks, 200, "");
                Long end = System.currentTimeMillis();
                log.debug(String.format("------%dms locks xid:%s:read:%s  write:%s", end - begin, transactionInfo.getXid(),
                        null == read.getPath() ? "" : String.join(",", read.getPath())
                        , null == write.getPath() ? "" : String.join(",", write.getPath())));
            } catch (Exception ex) {
                log.warn(String.format("acquire lock error xid:%s,read path:%s, write path:%s stack:%s", transactionInfo.getXid(),
                        null == read.getPath() ? "" : String.join(",", read.getPath())
                        , null == write.getPath() ? "" : String.join(",", write.getPath()),
                        BusinessException.exceptionFullMessage(ex)));
                sendLockResourcesResponse(locks, 500, ex.getMessage());
            }
        }
    }


}
