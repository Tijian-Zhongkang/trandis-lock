package com.xm.sanvanfo.trandiscore.netty.balancer.invoker;

import com.xm.sanvanfo.common.plugin.IPlugin;

public interface InvokerProvider extends IPlugin {

    Invoker provide();
}
