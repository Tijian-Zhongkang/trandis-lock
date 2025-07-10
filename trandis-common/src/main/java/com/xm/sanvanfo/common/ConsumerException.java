package com.xm.sanvanfo.common;

@FunctionalInterface
public interface ConsumerException<T> {
    void accept(T t) throws Exception;
}
