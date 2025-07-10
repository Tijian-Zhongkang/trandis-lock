package com.xm.sanvanfo.common;
@FunctionalInterface
public interface BiFunctionException<T,U,R> {
    R apply(T t, U u) throws Exception;
}
