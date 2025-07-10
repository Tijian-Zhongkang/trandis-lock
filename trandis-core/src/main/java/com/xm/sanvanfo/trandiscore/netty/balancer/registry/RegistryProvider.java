package com.xm.sanvanfo.trandiscore.netty.balancer.registry;

import com.xm.sanvanfo.common.plugin.IPlugin;

public interface RegistryProvider extends IPlugin {
    Registry provide();
}
