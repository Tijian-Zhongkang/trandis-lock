package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.trandiscore.TransactionInfo;
import com.xm.sanvanfo.trandiscore.transaction.BranchTransaction;
import com.xm.sanvanfo.trandiscore.transaction.CollectionBranchTransaction;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class GlobalTransactionInfo {
    private String xid;
    private String transactionName;
    private String applicationId;
    private Long timeout;
    private String groupName;
    private Integer retryTimes;
    private TransactionType branchTransactionType;

    public GlobalTransactionInfo() {

    }

    public GlobalTransactionInfo(TransactionInfo transactionInfo) {
        xid = transactionInfo.getXid();
        transactionName = transactionInfo.getTransactionName();
        applicationId = transactionInfo.getApplicationId();
       timeout = transactionInfo.getTimeout();
       groupName = transactionInfo.getGroupName();
       branchTransactionType = getTransactionTypeFromTransaction(transactionInfo.getBranchTransaction());
       retryTimes = transactionInfo.getRetryTimes();
    }

    private TransactionType getTransactionTypeFromTransaction(BranchTransaction branchTransaction) {
        TransactionType transactionType = new TransactionType();
        transactionType.setMainType(branchTransaction.getClass());
        if(branchTransaction instanceof CollectionBranchTransaction) {
            transactionType.setChildrenType(new ArrayList<>());
            Collection<BranchTransaction> collection = ((CollectionBranchTransaction)branchTransaction)
                    .getAllChildren();
            for (BranchTransaction transaction:collection
                 ) {
                transactionType.getChildrenType().add(getTransactionTypeFromTransaction(transaction));
            }

        }
        return transactionType;
    }
}
