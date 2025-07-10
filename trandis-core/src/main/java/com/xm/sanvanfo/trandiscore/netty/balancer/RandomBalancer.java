package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@CustomPlugin(registerClass = Balancer.class, name = "RandomBalancer")
public class RandomBalancer extends AbstractBalancer {

     RandomBalancer(Invoker invoker, BalancerConfiguration configuration, CacheStore cacheStore) {
        super(invoker, configuration, cacheStore);
    }

    @Override
    protected <T> T select(List<T> serverList, String xid) {
        int length = serverList.size();
        if(length == 0) {
            throw new BusinessException("no server found");
        }
        return serverList.get(ThreadLocalRandom.current().nextInt(length));
    }


}
