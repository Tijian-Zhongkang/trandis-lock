package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.trandiscore.transaction.BranchTransaction;

public interface IBranchTransactionFactoryCreator extends IBranchTransactionCreator {

    void register(Class<? extends BranchTransaction> transactionType, IBranchTransactionCreator creator);
}
