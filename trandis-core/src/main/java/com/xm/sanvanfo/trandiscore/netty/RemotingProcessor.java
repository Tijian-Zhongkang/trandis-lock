package com.xm.sanvanfo.trandiscore.netty;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public interface RemotingProcessor {

    void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception;

    default ExecutorService getExecutor() {return null;}

}
