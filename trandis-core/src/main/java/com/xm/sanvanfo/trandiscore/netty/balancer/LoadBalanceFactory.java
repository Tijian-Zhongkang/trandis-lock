package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.Invoker;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.InvokerListener;

import java.util.List;


public class LoadBalanceFactory {

    private static final LoadBalanceFactory instance = new LoadBalanceFactory();
    private String invokerType = "";
    private Integer virtualNumber = 64;

    public static LoadBalanceFactory INSTANCE() {
        return instance;
    }

    public void registerDefault(String invokerType, Integer virtualNumber, List<InvokerListener> listeners, CacheStore store) throws Exception {
        if(!this.invokerType.equals(invokerType)) {
            Invoker invoker = InvokerProviderFactory.INSTANCE().loadProvider(invokerType).provide();
            invoker.registerListeners(listeners);
             PluginLoader.INSTANCE().registerPlugin(new RandomBalancer(invoker,
                     new BalancerConfiguration(Balancer.BalanceType.RandomBalancer, 0), store));
             PluginLoader.INSTANCE().registerPlugin(new ConsistentHashBalancer(invoker,
                     new BalancerConfiguration(Balancer.BalanceType.ConsistentHashBalancer, virtualNumber), store));
             this.invokerType = invokerType;
        }
        else if(!this.virtualNumber.equals(virtualNumber)) {
           setVirtualNumber(virtualNumber);
        }
    }

    @SuppressWarnings({"WeakerAccess"})
    public void setVirtualNumber(Integer virtualNumber) throws Exception {
        Balancer balancer = PluginLoader.INSTANCE().load(Balancer.class, "ConsistentHashBalancer");
        if(null != balancer) {
            balancer.setVirtualNodeNumber(virtualNumber);
        }
        this.virtualNumber = virtualNumber;
    }

    @SuppressWarnings({"unused"})
    public void registerPlugin(Balancer... balancers) {
        for (Balancer balancer:balancers
             ) {
            PluginLoader.INSTANCE().registerPlugin(balancer);
        }

    }

    public Balancer loadBalancer(Balancer.BalanceType type) throws Exception {
        return PluginLoader.INSTANCE().load(Balancer.class, type.toString());
    }


}
