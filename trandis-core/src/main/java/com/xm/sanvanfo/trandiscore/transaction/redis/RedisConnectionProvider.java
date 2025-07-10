package com.xm.sanvanfo.trandiscore.transaction.redis;

import com.xm.sanvanfo.trandiscore.GlobalLockKey;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;

import java.util.List;

public interface RedisConnectionProvider {

    void flushUndoLog(String xid, String name);

    void flushUndoLog(String xid, String name, int sectionNumber);

    void executeUndoLog(String xid, String name, int sectionNumber);


    void removeVersion(String xid, String name, TransactionStatusEx statusEx);

    void removeVersion(String xid, String name, TransactionStatusEx statusEx, int sectionNumber);

    void updateImage(List<GlobalLockKey> lockKeys, String xid);

    String getSpaceName();

    void closeConnection();
}
