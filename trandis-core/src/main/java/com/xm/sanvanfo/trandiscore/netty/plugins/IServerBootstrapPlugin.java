package com.xm.sanvanfo.trandiscore.netty.plugins;

import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;

public interface IServerBootstrapPlugin extends IBootstrapHandlerPlugin {

    default void doConfiguration(NettyServerConfig config) {}

    default void doBindBefore(ServerBootstrap bootstrap){}

    default void doBindCompleted(ServerBootstrap bootstrap, NettyServerConfig config) {}

    default void doShutdown(NettyServerConfig config) {}
}
