package com.xm.sanvanfo.trandiscore.transaction.redis;

import lombok.Data;

import java.util.List;

@Data
public class RedisUndoLog {
    private String xid;
    private String applicationId;
    private Integer commandType;
    private String methodName;
    private List<RedisArg> args;
    private List<Class> argsClasses;
    //using for restoring global lock key
    private Object keyId;
    private Object subKeyId;
    private Integer sectionNumber;
    private String clientId;
    private boolean decorator = false;

}
