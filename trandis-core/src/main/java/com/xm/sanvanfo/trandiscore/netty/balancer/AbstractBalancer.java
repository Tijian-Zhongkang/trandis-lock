package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.Invoker;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBalancer implements Balancer {

    protected final Invoker invoker;
    protected final BalancerConfiguration configuration;
    private final CacheStore cacheStore;
    private final ConcurrentHashMap<String, Object> caches = new ConcurrentHashMap<>();

    AbstractBalancer(Invoker invoker, BalancerConfiguration configuration, CacheStore cacheStore) {
        this.invoker = invoker;
        this.configuration = configuration;
        this.cacheStore = cacheStore;
    }

    @Override
    public <T> List<T> getAvailableInvokers(String clusterName) throws Exception {
        return invoker.getAvailableInvokers(clusterName);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T select(String clusterName, String xid, boolean cached, boolean checkExists) throws Exception {
        List<T> list = getAvailableInvokers(clusterName);
        if(!cached) {
            return select(list, xid);
        }
       else {
           if(caches.containsKey(xid)) {
               T t = (T) caches.get(xid);
               if(!checkExists || list.contains(t)) {
                   return t;
               }

           }
           synchronized (this) {
               if(caches.containsKey(xid)) {
                   T t = (T) caches.get(xid);
                   if(!checkExists || list.contains(t)) {
                       return t;
                   }
               }
               T t = (T) cacheStore.get(xid);
               if (null != t) {
                   if(!checkExists || list.contains(t)) {
                       caches.put(xid, t);
                       return t;
                   }
               }
               t = select(list, xid);
               cacheStore.put(xid, (Serializable) t);
               caches.put(xid, t);
               return t;
           }
        }
    }

    @Override
    public void clearCache(String xid, boolean removeStore) {
        caches.remove(xid);
        if(removeStore) {
            cacheStore.remove(xid);
        }
    }

    @Override
    public void setVirtualNodeNumber(Integer num) {
        configuration.setVirtualNodeNumber(num);
    }

    protected abstract  <T> T select(List<T> serverList, String xid) throws Exception;
}
