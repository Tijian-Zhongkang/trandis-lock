package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.common.utils.RetryUtils;
import com.xm.sanvanfo.trandiscore.constant.TransactionConst;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.netty.TransactionBranchClientManager;
import com.xm.sanvanfo.trandiscore.protocol.request.*;
import com.xm.sanvanfo.trandiscore.protocol.response.*;
import com.xm.sanvanfo.trandiscore.session.GlobalTransactionInfo;
import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import com.xm.sanvanfo.trandiscore.session.UUIDGenerator;
import com.xm.sanvanfo.trandiscore.transaction.BranchTransaction;
import com.xm.sanvanfo.trandiscore.transaction.TransactionException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

@Slf4j
@SuppressWarnings({"WeakerAccess"})
public class TransactionManager {

    private static TransactionManager instance = new TransactionManager();

    private ThreadLocal<TransactionInfo> current = new ThreadLocal<>();

    private  ThreadLocal<Boolean> testRollback = new ThreadLocal<>();

    @Getter
    @Setter
    private String creatorName;

    private final ConcurrentHashMap<String, TransactionInfo> transactions = new ConcurrentHashMap<>();

    public static TransactionManager  INSTANCE() {
        return instance;
    }

    public void create(TransactionInfo info) {
        //send create to tc xid is created in client because balance cache
        info.setXid( info.getApplicationId() + "|" + UUIDGenerator.generateUUID());
        GlobalTransactionInfo transactionInfo = new GlobalTransactionInfo(info);
        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setApplicationId(info.getApplicationId());
        request.setClientId(info.getBranchTransaction().getClientId());
        request.setTransactionInfo(transactionInfo);
        RetryUtils.invokeRetryTimes("create", o-> {
            try {
                TransactionCreateResponse response =
                        (TransactionCreateResponse)TransactionBranchClientManager.INSTANCE()
                                .getRemotingClient().sendSyncRequest(request, true, false);

                if(!response.getCode().equals(200)) {
                    throw new BusinessException("create transaction error msg:" + response.getMsg());
                }
                current.set(info);
                transactions.put(info.getXid(), info);

            }
            catch (Exception ex) {
                throw new BusinessException(ex, "create transaction error");
            }
        }, info.getRetryTimes(), RetryUtils.RetryType.EXP, TransactionConst.waitMillis);
    }

    public void bind(TransactionInfo info) {
        current.set(info);
        transactions.put(info.getXid(), info);
    }

    public void testRollback() {
        testRollback.set(true);
    }

    public void begin(String xid) {
        BranchBeginRequest request = new BranchBeginRequest();

        sendAbstractRequest(xid, request, "begin", TransactionStatusEx.BeginFail, false, (t,o)->{
            Asserts.isTrue(o instanceof BranchBeginResponse);
            BranchBeginResponse response = (BranchBeginResponse)o;
            t.setCurrentSection(response.getSectionNumber());
        });
    }

    public void commit(String xid) {
        TransactionInfo info = transactions.get(xid);
        try {
            BranchCommitRequest commitRequest = new BranchCommitRequest();
            commitRequest.setSectionNumber(info.getCurrentSection());
            sendAbstractRequest(xid, commitRequest, "commit", TransactionStatusEx.CommitFailed, false, (t, o) ->
                log.debug(String.format("global transaction were committed xid is %s, name is %s, server id is %s",
                        t.getXid(), t.getTransactionName(), o.getServerId())));
        }
        finally {

            TransactionBranchClientManager.INSTANCE().getRemotingClient().clearBalanceCache(xid, info.getBranchTransaction().isTrunk());
        }
    }

    public void rollback(String xid) {
        TransactionInfo info = transactions.get(xid);
        try {
            BranchRollbackRequest rollbackRequest = new BranchRollbackRequest();
            rollbackRequest.setSectionNumber(info.getCurrentSection());
            sendAbstractRequest(xid, rollbackRequest, "rollback", TransactionStatusEx.RollbackFailed,
                    info.getBranchTransaction().isTrunk(), (t, o) ->
                log.debug(String.format("global transaction were rolled back xid is %s, name is %s, server id is %s",
                        t.getXid(), t.getTransactionName(), o.getServerId()))
            );
        }
        finally {
            TransactionBranchClientManager.INSTANCE().getRemotingClient().clearBalanceCache(xid, info.getBranchTransaction().isTrunk());
        }
    }

    public void branchRollback(String xid, Integer section) {
        TransactionInfo transactionInfo = getAndBind(xid, TransactionStatusEx.RollbackFailed);
        transactionInfo.getBranchTransaction().branchRollback(section);
        transactionInfo.setCurrentSection(section);
    }

    public void branchCommit(String xid) {
        Long begin = System.currentTimeMillis();
        TransactionInfo transactionInfo = getAndBind(xid, TransactionStatusEx.CommitFailed);
        transactionInfo.getBranchTransaction().branchCommit();
        Long end = System.currentTimeMillis();
        log.debug(String.format("-----------%dms branch commit xid:%s", end - begin, xid));
    }

    public void setSectionNumber(String xid, Integer sectionNumber) {
        TransactionInfo transactionInfo = transactions.get(xid);
        if(null == transactionInfo) {
            throw new TransactionException("transaction is null:" + xid, TransactionStatusEx.CommitFailed);
        }
        transactionInfo.setCurrentSection(sectionNumber);
    }

    public boolean isTransactionActive() {
        return current.get() != null;
    }

    public void clearCurrent() {
        TransactionInfo info = current.get();
        if(null != info) {
            current.set(null);
            transactions.remove(info.getXid());
        }
    }

    public TransactionInfo getCurrentTransactionInfo() {return current.get();}


    public TransactionInfo getFromLocalAndServer(String xid, TransactionStatusEx status) {
        TransactionInfo info = transactions.get(xid);
        if(null != info) {
            return info;
        }

        return RetryUtils.invokeRetryTimes("getFromLocalAndServer", () ->{

            GetTransactionInfoRequest request = new GetTransactionInfoRequest();
            try {
                IBranchTransactionCreator creator = getCreator();
                request.setXid(xid);
                request.setApplicationId(creator.applicationId());
                request.setClientId(creator.clientId());
                GetTransactionInfoResponse response = (GetTransactionInfoResponse) TransactionBranchClientManager.INSTANCE().getRemotingClient().
                        sendSyncRequest(request, true, false);
                if(!response.getCode().equals(200)) {
                    throw new TransactionException(String.format("global transaction xid %s application:%s get info error:%s",
                            xid, request.getApplicationId(), response.getMsg()),
                            status);
                }
                GlobalTransactionInfo transactionInfo = response.getInfo();
                Boolean trunk = response.getTrunk();
                return creator.create(transactionInfo, trunk);

            }
            catch (InterruptedException ex) {
                throw new TransactionException(String.format("global transaction xid %s application:%s get info interrupted error:%s",
                        xid, request.getApplicationId(), ex.getMessage()), ex,
                        status);
            }
            catch (TimeoutException ex) {
                throw new TransactionException(String.format("global transaction xid %s application:%s get info timeout error:%s",
                        xid, request.getApplicationId(), ex.getMessage()), ex,
                        status);
            }
            catch (Exception ex) {
                throw new TransactionException(String.format("global transaction xid %s application:%s get info error:%s",
                        xid, request.getApplicationId(), ex.getMessage()), ex,
                        status);
            }
        }, null, TransactionConst.RetryTimes, RetryUtils.RetryType.EXP, 1000);


    }

    private IBranchTransactionCreator getCreator() throws Exception {
        return PluginLoader.INSTANCE().load(IBranchTransactionCreator.class, creatorName);
    }

    private TransactionInfo getAndBind(String xid, TransactionStatusEx status) {
        TransactionInfo info = getFromLocalAndServer(xid, status);
        transactions.putIfAbsent(xid, info);
        return info;
    }

    private void sendAbstractRequest(String xid, BranchAbstractRequest request, String step, TransactionStatusEx status, boolean checkNodeExists,
                                     BiConsumer<TransactionInfo, BranchAbstractResponse> consumer) {
        TransactionInfo transactionInfo = transactions.get(xid);
        BranchTransaction transaction = transactionInfo.getBranchTransaction();
        transaction.setDefaultRequest(request);
        RetryUtils.invokeRetryTimes("send" + request.getClass().getSimpleName(), o-> {
            try {
                BranchAbstractResponse response = (BranchAbstractResponse) TransactionBranchClientManager.INSTANCE().getRemotingClient().
                        sendSyncRequest(request, true, checkNodeExists);
                if(null == response) {
                    throw new TransactionException(String.format("global transaction %s: branch transaction %s %s branch error:%s",
                            transactionInfo.getTransactionName(), transaction.getName(), step, "send timeout"),
                            status);
                }
                if(!response.getCode().equals(200)) {
                    throw new TransactionException(String.format("global transaction %s: branch transaction %s %s branch error:%s",
                            transactionInfo.getTransactionName(), transaction.getName(), step, response.getMsg()),
                            status);
                }
                if(null != consumer) {
                    consumer.accept(transactionInfo,response);
                }
            }
            catch (InterruptedException ex) {
                throw new TransactionException(String.format("global transaction %s: branch transaction %s %s branch begin interrupted",
                        transactionInfo.getTransactionName(), transaction.getName(), step), ex, status);
            }
            catch (TimeoutException ex) {
                throw new TransactionException(String.format("global transaction %s: branch transaction %s %s branch begin timeout",
                        transactionInfo.getTransactionName(), transaction.getName(), step), ex, status);
            }
        },  transactionInfo.getRetryTimes(),  RetryUtils.RetryType.EXP, 1000);
    }

    public void lockResource(String lockId, String xid, List<String> read, List<String> write, Long timeout) {

        resourceSet(lockId, xid, read,write, timeout, BranchLockResourceRequest.LockType.Lock, "lock resource");
    }

    public void releaseReadResource(String lockId, String xid, List<String> read, List<String> write, Long timeout) {
        resourceSet(lockId, xid, read,write, timeout, BranchLockResourceRequest.LockType.ReleaseRead, "release read resource");
    }

    public void releaseResource(BranchLockResourceRequest request) {
        sendAbstractRequest(request.getXid(), request, "future no found release lock", TransactionStatusEx.CommitFailed, false, (t, o) ->
                log.debug(String.format("release lock %s", request.toString())));
    }

    private void resourceSet(String lockId, String xid, List<String> read, List<String> write, Long timeout, BranchLockResourceRequest.LockType type,
                             String step) {
        BranchLockResourceRequest request = new BranchLockResourceRequest();
        Map<Boolean, ReadWriteLockPath.LockPath> map = new HashMap<>();
        map.put(false, new ReadWriteLockPath.LockPath(read, timeout, TimeUnit.SECONDS));
        map.put(true, new ReadWriteLockPath.LockPath(write, timeout, TimeUnit.SECONDS));
        request.setReadWrite(map);
        request.setLockType(type);
        request.setLockId(lockId);
        sendAbstractRequest(xid, request, step, TransactionStatusEx.CommitFailed, false, (t, o) ->
                log.debug(String.format("-------- transaction lock type:%s success xid:%s, read:%s, write:%s", type.toString(),xid,
                        null == read ? "" : String.join(",", read),
                        null == write ? "" : String.join(",", write))));
    }

    public Set<String> getLocks(String xid) {
        GetTransactionLocksRequest request = new GetTransactionLocksRequest();
        ObjectHolder<Set<String>> setObjectHolder = new ObjectHolder<>();
        setObjectHolder.setObj(new HashSet<>());
        sendAbstractRequest(xid, request, "get locks", TransactionStatusEx.CommitFailed, false, (t,o) ->
        {
            log.debug(String.format("--------- transaction get locks %s", o.toString()));
            setObjectHolder.setObj(((GetTransactionLocksResponse)o).getLocks());
        });
        return setObjectHolder.getObj();
    }
}
