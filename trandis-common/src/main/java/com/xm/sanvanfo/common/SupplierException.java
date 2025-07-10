package com.xm.sanvanfo.common;

@FunctionalInterface
public interface SupplierException<T> {

    T get() throws Exception;
}
