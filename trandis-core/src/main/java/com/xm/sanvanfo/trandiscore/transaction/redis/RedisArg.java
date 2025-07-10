package com.xm.sanvanfo.trandiscore.transaction.redis;

import lombok.Data;

@Data
public class RedisArg {
    private String stringVal;
    private String methodConvert;
    private Class objClass;
    private Boolean classStaticMethod;
}
