package com.xm.sanvanfo.trandiscore;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class TransactionMethodInfo {

    private Method methodInfo;
    private Object target;
    private Object decorator;
    private Object[] args;
    private Object[] snapshotArgs;
}
