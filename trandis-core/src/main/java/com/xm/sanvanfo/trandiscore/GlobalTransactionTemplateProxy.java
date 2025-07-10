package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.transaction.GlobalTransactionExecutor;
import com.xm.sanvanfo.trandiscore.transaction.TransactionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GlobalTransactionTemplateProxy {

    public Object execute(GlobalTransactionExecutor executor) {

        long begin = System.currentTimeMillis();
        TransactionInfo transactionInfo = executor.newTransactionInfo();
        if(StringUtils.isEmpty(transactionInfo.getXid())) {
            TransactionManager.INSTANCE().create(transactionInfo);
        }
        else {
            //had global transaction
            TransactionManager.INSTANCE().bind(transactionInfo);
        }
        long end = System.currentTimeMillis();
        log.debug("-----------transaction create：" + (end - begin) + "ms");

        Object o;
        try {
            begin = System.currentTimeMillis();
            if (transactionInfo.getTimeout() != null && StringUtils.isNotEmpty(transactionInfo.getGroupName())) {
                transactionInfo.getBranchTransaction().begin(transactionInfo.getTimeout(),
                        transactionInfo.getGroupName());
            } else if (transactionInfo.getTimeout() != null) {
                transactionInfo.getBranchTransaction().begin(transactionInfo.getTimeout());
            } else {
                transactionInfo.getBranchTransaction().begin();
            }
            end = System.currentTimeMillis();
            log.debug("-----------transaction local begin：" + (end - begin) + "ms");
            begin = System.currentTimeMillis();
            TransactionManager.INSTANCE().begin(transactionInfo.getXid());
            end = System.currentTimeMillis();
            log.debug("-----------transaction remote begin：" + (end - begin) + "ms");
            begin = System.currentTimeMillis();
            o = executor.execute();
            transactionInfo.getBranchTransaction().commit();
            end = System.currentTimeMillis();
            log.debug("-----------transaction local execute：" + (end - begin) + "ms");
            begin = System.currentTimeMillis();
            TransactionManager.INSTANCE().commit(transactionInfo.getXid());
            end = System.currentTimeMillis();
            log.debug("-----------transaction commit：" + (end - begin) + "ms");
            return o;
        } catch (Throwable ex) {
            try {
                transactionInfo.getBranchTransaction().rollback();
                TransactionManager.INSTANCE().rollback(transactionInfo.getXid());
                log.warn(String.format("exception: xid:%s, message:%s", transactionInfo.getXid(), ex.getMessage()), ex);
                throw ex;
            } catch (Throwable e) {
                throw new TransactionException(e.getMessage(), e, !transactionInfo.getBranchTransaction().isTrunk() ?
                        TransactionStatusEx.CommitFailedThrowing : TransactionStatusEx.CommitFailed);
            }

        }
        finally {
            transactionInfo.getBranchTransaction().returnResource();
            TransactionManager.INSTANCE().clearCurrent();
            log.debug(String.format("------transaction %s xid:%s end", transactionInfo.getTransactionName(), transactionInfo.getXid()));
        }
    }
}
