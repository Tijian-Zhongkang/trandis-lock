package com.xm.sanvanfo.trandiscore.netty.plugins;

import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

public interface IClientBootstrapPlugin extends IBootstrapHandlerPlugin {

    default void doConfiguration(NettyClientConfig config) {}

    default void doStarted(Bootstrap bootstrap) {}

    default void doConnectCompleted(Bootstrap bootstrap, InetSocketAddress address, Channel channel){}

}
