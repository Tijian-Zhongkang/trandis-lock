package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.GlobalLockKey;
import com.xm.sanvanfo.trandiscore.TransactionFuncInterceptor;
import com.xm.sanvanfo.trandiscore.TransactionInfo;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.protocol.LockStepType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public class CollectionBranchTransaction extends AbstractBranchTransaction {

    private final ConcurrentLinkedQueue<BranchTransaction> children = new ConcurrentLinkedQueue<>();
    public CollectionBranchTransaction(TransactionInfo transactionInfo, Collection<? extends BranchTransaction> children, TransactionLevelEx levelEx) {
        super(levelEx);
        this.transactionInfo = transactionInfo;
        this.children.addAll(children);
    }

    public Collection<BranchTransaction> getAllChildren() {
        return Collections.unmodifiableCollection(children);
    }

    @Override
    public String getName() {
        return transactionInfo.getTransactionName();
    }


    @Override
    public void begin() throws TransactionException {
        begin(getTimeout());
    }

    @Override
    public void begin(long timeout) throws TransactionException {
         begin(timeout,transactionInfo.getTransactionName());
    }

    @Override
    public void begin(long timeout, String name) throws TransactionException {
        try {
            doFunc(new Object[]{timeout, name}, AbstractSingleBranchTransaction.class.getDeclaredMethod("beginInner", long.class, String.class)
                  , AbstractSingleBranchTransaction.class.getDeclaredMethod("getBeginLockKeys"), TransactionStatusEx.BeginFail);
        }
        catch (Exception ex) {
            throw new TransactionException(ex.getMessage(), ex, TransactionStatusEx.BeginFail);
        }
    }

    @Override
    public void commit() throws TransactionException{
        try {
            doFunc(null, AbstractSingleBranchTransaction.class.getDeclaredMethod("commit"),
                    AbstractSingleBranchTransaction.class.getDeclaredMethod("getCommitLockKeys"), TransactionStatusEx.CommitFailed);
        }
        catch (Exception ex) {
            throw new TransactionException(ex.getMessage(), ex, TransactionStatusEx.CommitFailed);
        }
    }

    @Override
    public void branchRollback(int section) throws TransactionException {
        releaseFunc((lockKeys, o) -> {
            lockKeys.addAll(o.getLocks());
            o.executeUndoLogs(section);
        });
    }


    @Override
    public void addInterceptor(TransactionFuncInterceptor interceptor) {
        BranchTransaction transaction = find(interceptor.getSpaceName(), this);
        if(null != transaction) {
            transaction.addInterceptor(interceptor);
        }
        else {
            throw new BusinessException(String.format("space %s is not found", interceptor.getSpaceName()));
        }
    }

    private BranchTransaction find(String spaceName, CollectionBranchTransaction collections) {
        for (BranchTransaction b:collections.children
        ) {
            if(b instanceof CollectionBranchTransaction) {
                BranchTransaction find =  find(spaceName, (CollectionBranchTransaction)b);
                if(find != null) {
                    return find;
                }
            }
            else if(b.getSpaceName().equals(spaceName)) {
                return b;
            }
        }
        return null;
    }

    @Override
    public String getSpaceName() {
        return null;
    }

    @Override
    public void rollback() throws TransactionException {
        releaseFunc((lockKeys, o) -> {
            lockKeys.addAll(o.getLocks());
            o.executeUndoLogs();
        });
    }

    @Override
    public void returnResource() {
        for (BranchTransaction o:children
        ) {
            o.returnResource();
        }
    }

    @Override
    public void branchCommit() throws TransactionException {
        releaseFunc((lockKeys, o) -> {
            lockKeys.addAll(o.getLocks());
            o.removeUndoLogs();
        });
    }

    @Override
    public TransactionStatusEx getStatus() {
        TransactionStatusEx min = TransactionStatusEx.Finished;
        for (BranchTransaction o:children
             ) {
            TransactionStatusEx status = o.getStatus();
             if(min.ordinal() > status.ordinal()) {
                 min = status;
             }
        }
        return min;
    }



    private void releaseFunc(BiConsumer<List<GlobalLockKey>, AbstractSingleBranchTransaction> func) {
        List<GlobalLockKey> lockKeys = new ArrayList<>();
        Exception exThrow = null;
        for (BranchTransaction o:children
        ) {
            try {
                if(AbstractSingleBranchTransaction.class.isAssignableFrom(o.getClass())) {
                    func.accept(lockKeys, (AbstractSingleBranchTransaction) o);
                }
            }
            catch (Exception ex){
                exThrow = ex;
            }
        }
        if(null != exThrow) {
            if(exThrow instanceof TransactionException) {
                throw (TransactionException)exThrow;
            }
            throw new TransactionException(exThrow.getMessage(),  exThrow, TransactionStatusEx.CommitFailed);
        }
    }

    private Integer getTimeout() {
        Integer timeout = 0;
        for (BranchTransaction o : children) {
            if (AbstractSingleBranchTransaction.class.isAssignableFrom(o.getClass())) {
                Integer transactionTimeout = ((AbstractSingleBranchTransaction) o).getDefaultTimeout();
                if (transactionTimeout > timeout) {
                    timeout = transactionTimeout;
                }
            }
        }
        return timeout;
    }

    @SuppressWarnings({"unchecked"})
    private void doFunc(Object[] objs, Method m, Method getLocks,  TransactionStatusEx status) throws TransactionException {
        Map<String, GlobalLockKey> lockKeys = new HashMap<>();
        Integer timeout = 0;
        if(null != getLocks) {
            for (BranchTransaction o : children) {
                try {
                    if (AbstractSingleBranchTransaction.class.isAssignableFrom(o.getClass())) {
                        Object locks = getLocks.invoke(o);
                        if (null != locks) {
                            lockKeys.putAll((Map<String, GlobalLockKey>) locks);
                        }
                        Integer transactionTimeout = ((AbstractSingleBranchTransaction) o).getDefaultTimeout();
                        if (transactionTimeout > timeout) {
                            timeout = transactionTimeout;
                        }
                    }
                } catch (Exception ex) {
                    throw new TransactionException(ex.getMessage(), ex, status);
                }
            }
        }
        for(BranchTransaction o : children) {
            try {
                if(null == objs) {
                    m.invoke(o);
                }
                else {
                    m.invoke(o, objs);
                }
            }
            catch (Exception ex) {
                if(ex.getCause() instanceof TransactionException) {
                    throw (TransactionException)ex.getCause();
                }
                throw new TransactionException(ex.getCause().getMessage(),  ex, status);
            }
        }
    }


    @Override
    protected String getApplicationId() {
        return null;
    }


}
