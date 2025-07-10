package com.xm.sanvanfo.trandiscore.netty.balancer.registry;

import java.net.InetSocketAddress;

public interface Registry {

    void register(InetSocketAddress address, String clusterName) throws Exception;

    void unregister(InetSocketAddress address, String clusterName) throws Exception;
}
