package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;

public interface Transaction {

    String getName();

    void begin() throws TransactionException;

    void begin(long timeout) throws TransactionException;

    void begin(long timeout, String name) throws TransactionException;

    void commit() throws TransactionException;

    void rollback() throws TransactionException;

    void returnResource();

    TransactionStatusEx getStatus();

}
