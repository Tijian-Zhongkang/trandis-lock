package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.InvokerProvider;

public class InvokerProviderFactory {

    private static final InvokerProviderFactory instance = new InvokerProviderFactory();

    public static InvokerProviderFactory INSTANCE() {
        return instance;
    }

    @SuppressWarnings({"unused"})
    public void registerPlugin(InvokerProvider... providers) {
        for (InvokerProvider provider:providers
             ) {
            PluginLoader.INSTANCE().registerPlugin(provider);
        }
    }

    public InvokerProvider loadProvider(String provider) throws Exception{
        return PluginLoader.INSTANCE().load(InvokerProvider.class, provider);
    }
}
