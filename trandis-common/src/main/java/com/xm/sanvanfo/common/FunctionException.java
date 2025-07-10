package com.xm.sanvanfo.common;

@FunctionalInterface
public interface FunctionException<T, R> {
    R apply(T t) throws Exception;
}
