package com.xm.sanvanfo.trandiscore.netty.balancer.invoker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract   class AbstractInvoker implements Invoker{

    protected List<InvokerListener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void registerListeners(List<InvokerListener> listeners) {
        this.listeners.addAll(listeners);
    }

    protected void refreshApply() {
        for (InvokerListener listener:listeners
             ) {
            listener.apply();
        }
    }
}
