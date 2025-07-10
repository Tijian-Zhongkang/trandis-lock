package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.netty.balancer.registry.RegistryProvider;

public class RegisterProviderFactory {
    private  static final  RegisterProviderFactory instance = new RegisterProviderFactory();

    @SuppressWarnings({"unused"})
    public void registerRegistryProviders(RegistryProvider... providers) {
        for (RegistryProvider provider:providers
             ) {
            PluginLoader.INSTANCE().registerPlugin(provider);
        }
    }

    public static RegisterProviderFactory INSTANCE() {
        return instance;
    }

    public RegistryProvider  loadProvider(String provider) throws Exception {
        return PluginLoader.INSTANCE().load(RegistryProvider.class, provider);
    }
}
