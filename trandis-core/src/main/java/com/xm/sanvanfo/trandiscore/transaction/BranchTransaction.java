package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.TransactionFuncInterceptor;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchAbstractRequest;

public interface BranchTransaction extends Transaction {

    void branchCommit() throws TransactionException;

    void branchRollback(int section) throws TransactionException;

    boolean isTrunk();

    void setDefaultRequest(BranchAbstractRequest request);

    void addInterceptor(TransactionFuncInterceptor interceptor);

    String getSpaceName();

    String getClientId();

}
