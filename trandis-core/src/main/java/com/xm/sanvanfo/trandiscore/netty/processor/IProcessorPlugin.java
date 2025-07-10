package com.xm.sanvanfo.trandiscore.netty.processor;

import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public interface IProcessorPlugin extends IPlugin {

    default void  process(AbstractNettyRemoting remoting, ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {}

    Object deserialize(byte[] bytes, byte codec) throws Exception;

    byte[] serialize(Object obj, byte codec) throws Exception;

    default ExecutorService getExecutor() {return null;}
}
