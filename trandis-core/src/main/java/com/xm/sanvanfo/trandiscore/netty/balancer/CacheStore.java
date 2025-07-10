package com.xm.sanvanfo.trandiscore.netty.balancer;

import java.io.Serializable;

public interface CacheStore {
    void put(String key, Serializable t);
    Serializable get(String key);
    void remove(String key);
}
