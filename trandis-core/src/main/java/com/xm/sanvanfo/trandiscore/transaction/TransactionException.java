package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;

public class TransactionException extends BusinessException {

    private TransactionStatusEx status;

    public TransactionException(String message, TransactionStatusEx status) {
        super(message);
        this.status = status;
    }

    public TransactionException(String message, Throwable ex, TransactionStatusEx status) {
        super(ex, message);
        this.status = status;
    }

    public TransactionStatusEx getStatus() {
        return status;
    }
}
