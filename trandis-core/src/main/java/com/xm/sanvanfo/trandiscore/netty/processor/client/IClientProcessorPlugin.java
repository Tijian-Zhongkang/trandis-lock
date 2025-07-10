package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import io.netty.channel.ChannelHandlerContext;

public interface IClientProcessorPlugin extends IProcessorPlugin {

    default void  process(AbstractNettyRemoting remoting, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        remoting.processMessageResult(rpcMessage);
    }

    void setParameters(AbstractNettyRemoting client, String applicationId, String address);
}
