package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.TransactionInfo;

public interface TransactionExecutor {

    Object execute() throws Throwable;

    TransactionInfo newTransactionInfo();
}
