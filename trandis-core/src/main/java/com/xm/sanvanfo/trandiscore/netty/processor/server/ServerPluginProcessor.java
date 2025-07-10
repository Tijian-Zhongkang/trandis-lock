package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import io.netty.channel.ChannelHandlerContext;

interface ServerPluginProcessor {

    default void setPluginParameters(IProcessorPlugin plugin, ChannelHandlerContext ctx, RpcMessage rpcMessage, AbstractNettyRemoting remoting, String serverId) {
        Asserts.isTrue(IServerProcessorPlugin.class.isAssignableFrom(plugin.getClass()));
        IServerProcessorPlugin serverProcessorPlugin = (IServerProcessorPlugin)plugin;
        serverProcessorPlugin.setParameters(remoting, serverId);
        NettyPoolKey poolKey = serverProcessorPlugin.getNettyPoolKey(ctx, rpcMessage);
        NettyChannel nettyChannel = new NettyChannel(poolKey, ctx.channel());
        Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
        ((AbstractNettyRemotingServer)remoting).updateChannel(nettyChannel);
    }
}
