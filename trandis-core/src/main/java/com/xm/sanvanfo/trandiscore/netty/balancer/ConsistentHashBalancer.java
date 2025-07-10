package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.ConsistentHashSelector;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.Invoker;

import java.util.List;


@CustomPlugin(registerClass = Balancer.class, name = "ConsistentHashBalancer")
public class ConsistentHashBalancer extends AbstractBalancer {

    ConsistentHashBalancer(Invoker invoker, BalancerConfiguration configuration, CacheStore cacheStore) {
        super(invoker, configuration, cacheStore);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected <T> T select(List<T> serverList, String xid) {
        if(serverList.size() == 0) {
            throw new BusinessException("no server found");
        }
        return (T)ConsistentHashSelector.Builder.create().setNodes(serverList.toArray(),
                configuration.getVirtualNodeNumber()).select(xid);
    }

}
