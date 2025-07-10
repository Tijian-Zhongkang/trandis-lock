package com.xm.sanvanfo.trandiscore.netty.balancer.invoker;

import java.util.List;

public interface Invoker {

    <T> List<T> getAvailableInvokers(String clusterName) throws Exception;

    void registerListeners(List<InvokerListener> listeners);
}
