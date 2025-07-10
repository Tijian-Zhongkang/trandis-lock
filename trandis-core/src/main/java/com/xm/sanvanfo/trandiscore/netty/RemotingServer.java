package com.xm.sanvanfo.trandiscore.netty;


import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

public interface RemotingServer {

    Object sendSyncRequest(NettyPoolKey key, Object msg) throws TimeoutException, InterruptedException;

    Object sendSyncRequestSampleApplication(NettyPoolKey key, Object msg) throws TimeoutException, InterruptedException;

    Object sendSyncRequest(NettyChannel channel, Object msg) throws TimeoutException, InterruptedException;


    @SuppressWarnings({"unused"})
    MessageFuture sendAsyncRequest(NettyChannel channel, Object msg);

    @SuppressWarnings("unused")
    MessageFuture sendAsyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel, MessageFuture> failConsumer);


    void sendAsyncResponse(RpcMessage rpcMessage, NettyChannel channel, Object msg);

    void sendAsyncResponse(RpcMessage rpcMessage, NettyChannel channel, Object msg, BiConsumer<NettyChannel, NettyResponse> failConsumer);

}
