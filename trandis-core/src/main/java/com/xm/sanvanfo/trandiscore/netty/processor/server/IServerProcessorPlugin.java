package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import io.netty.channel.ChannelHandlerContext;

public interface IServerProcessorPlugin extends IProcessorPlugin {


    void setParameters(AbstractNettyRemoting server, String serverId);

    NettyPoolKey getNettyPoolKey(ChannelHandlerContext ctx, RpcMessage rpcMessage);
}
