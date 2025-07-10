package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.trandiscore.session.GlobalTransactionInfo;

public interface IBranchTransactionCreator extends IPlugin {

    TransactionInfo create(GlobalTransactionInfo transactionInfo, Boolean trunk);

    String applicationId();

    String clientId();

    default TransactionInfo transform(GlobalTransactionInfo transactionInfo) {
        TransactionInfo info = new TransactionInfo();
        info.setTransactionName(transactionInfo.getTransactionName());
        info.setXid(transactionInfo.getXid());
        info.setRetryTimes(transactionInfo.getRetryTimes());
        info.setGroupName(transactionInfo.getGroupName());
        info.setApplicationId(transactionInfo.getApplicationId());
        info.setTimeout(transactionInfo.getTimeout());
        info.setCurrentSection(1);
        return info;
    }

}
