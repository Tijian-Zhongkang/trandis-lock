package com.xm.sanvanfo.common.utils;

public class ObjectHolder<T> {
    private T obj;

    public ObjectHolder() {}

    public ObjectHolder(T obj) {
        this.obj = obj;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T t) {
        obj = t;
    }
}
