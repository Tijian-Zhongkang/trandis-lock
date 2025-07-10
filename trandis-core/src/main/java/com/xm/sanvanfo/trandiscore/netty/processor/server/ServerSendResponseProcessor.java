package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RemotingProcessor;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

abstract class ServerSendResponseProcessor implements RemotingProcessor {

    protected final AbstractNettyRemotingServer server;
    protected final String serverId;
    protected final ExecutorService executorService;


    ServerSendResponseProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        this.server = server;
        this.serverId = serverId;
        this.executorService = executorService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        ObjectHolder<NettyChannel> channelHolder = new ObjectHolder<>();
        MessageTypeAware response = processToResponse(ctx, rpcMessage, channelHolder, true);
        if(null != response) {
            server.sendAsyncResponse(rpcMessage, channelHolder.getObj(), response);
        }

    }

    @Override
    public ExecutorService getExecutor() {
        return executorService;
    }

    protected abstract MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                               ObjectHolder<NettyChannel> channelHolder, boolean updateChannel) throws Exception;
}
