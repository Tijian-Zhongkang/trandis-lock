package com.xm.sanvanfo.trandiscore.netty.processor;

import com.xm.sanvanfo.trandiscore.netty.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public abstract class ResponseProcessor implements RemotingProcessor {

    protected final AbstractNettyRemoting remoting;
    protected final ExecutorService executorService;

    public ResponseProcessor(AbstractNettyRemoting remoting, ExecutorService executorService) {
        this.remoting = remoting;
        this.executorService = executorService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        remoting.processMessageResult(rpcMessage);
    }

    public ExecutorService getExecutor() {return executorService;}
}
