package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.*;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess"})
@Slf4j
public abstract class AbstractSingleBranchTransaction extends AbstractBranchTransaction {


    @Setter
    protected TransactionStatusEx status;
    protected ExecutorService commitService;

    protected final ConcurrentLinkedQueue<TransactionFuncInterceptor> interceptors = new ConcurrentLinkedQueue<>();


    public AbstractSingleBranchTransaction(TransactionInfo transactionInfo, Boolean trunk, TransactionLevelEx levelEx) {
        super(levelEx);
        this.transactionInfo = transactionInfo;
        this.trunk = trunk;
        if(transactionInfo.getAsyncCommit()) {
            commitService = new ThreadPoolExecutor(transactionInfo.getAsyncThreadNum(), 1024, 0L,
                    TimeUnit.SECONDS,  new LinkedBlockingQueue<>(1024));
        }
    }

    public void addInterceptor(TransactionFuncInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    public Collection<TransactionFuncInterceptor> getInterceptors() {
        return Collections.unmodifiableCollection(interceptors);
    }


    @Override
    public String getName() {
        return transactionInfo.getTransactionName();
    }


    public TransactionStatusEx getStatus() {
        return status;
    }

    @Override
    public void branchCommit() throws TransactionException {
        if(transactionInfo.getAsyncCommit()) {
            commitService.execute(()->{
                try {
                    branchCommitInner();
                }
            catch (TransactionException ex) {
                    log.warn(String.format("xid:%s branchCommit error, %s", transactionInfo.getXid(),
                            ex.getStackMessage()));
                }
            });
        }
        else {
            branchCommitInner();
        }
    }

    @Override
    public void begin(long timeout, String name) throws TransactionException {
        //send begin to server
        beginInner(timeout, name);
        status = TransactionStatusEx.Begin;
    }

    @Override
    public void commit() throws TransactionException {
        commitInner();
        status = TransactionStatusEx.Committed;
    }

    private void branchCommitInner() throws TransactionException {
        log.debug(String.format("---------branch xid : %s commit", transactionInfo.getXid()));
        removeVersion(TransactionStatusEx.Committed);
        removeUndoLogs();
    }

    @Override
    public void rollback() throws TransactionException {
        rollbackInner();
        removeVersion(transactionInfo.getCurrentSection(), TransactionStatusEx.RollbackFailed);
        status = TransactionStatusEx.Rollbacked;
    }


    @Override
    public void branchRollback(int section) throws TransactionException {
        log.debug(String.format("----------branch xid : %s section:%d rollback", transactionInfo.getXid(), section));
        executeUndoLogs(section);
        removeVersion(section, TransactionStatusEx.RollbackFailed);
        removeUndoLogs(section);
    }


    protected List<GlobalLockKey> getLocks() {

        Map<String,  GlobalLockKey> map = getLockMaps();
        List<GlobalLockKey> keys = new ArrayList<>(map.values());
        keys.sort(Comparator.comparingInt(GlobalLockKey::getOrderBy));
        return  keys;
    }

    protected List<GlobalLockKey> getLocks(Integer sectionNumber) {
        Map<String,  GlobalLockKey> map = getLockMaps();
        List<GlobalLockKey> keys = new ArrayList<>(map.values());
        keys.sort(Comparator.comparingInt(GlobalLockKey::getOrderBy));
        return keys.stream().filter(o->o.getSectionNumber().equals(sectionNumber)).collect(Collectors.toList());
    }

    protected Map<String,  GlobalLockKey> getLockMaps() {
        Map<String,  GlobalLockKey> map = new HashMap<>();
        int order = 1;
        for (TransactionFuncInterceptor func : getInterceptors()
        ) {
            if(null != func.getLockKeys()) {
                final Integer t = order;
                func.getLockKeys().forEach((u, v) -> {
                    v.setOrderBy(t);
                    if(!map.containsKey(u)) {
                        map.put(u, v);
                    }
                    else if(v.getWrite()) {
                        map.put(u, v);
                    }
                });
            }
            order++;
        }
        return map;
    }

    protected abstract void rollbackInner();

    protected abstract void beginInner(long timeout, String name);

    protected abstract void commitInner();

    protected abstract Integer getDefaultTimeout();

    protected abstract void removeUndoLogs();

    protected abstract void removeUndoLogs(int section);

    protected abstract void executeUndoLogs();

    protected abstract void executeUndoLogs(int section);

    protected abstract void removeVersion(TransactionStatusEx statusEx);

    protected abstract void removeVersion(int section, TransactionStatusEx statusEx);

}
