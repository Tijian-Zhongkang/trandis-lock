package com.xm.sanvanfo.trandiscore.netty.plugins;

import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public interface IBootstrapHandlerPlugin  {

    default void process(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {}

    default void destroy(ChannelHandlerContext ctx) throws Exception {}

    default void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception {}

    default void handleEvent(ChannelHandlerContext ctx, IdleStateEvent event) throws Exception {}

    int getOrderBy();
}
