package com.xm.sanvanfo.common;
@FunctionalInterface
public interface BiConsumerException<T, U> {
    void accept(T t, U u) throws Exception;
}
