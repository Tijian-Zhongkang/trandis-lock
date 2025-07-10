package com.xm.sanvanfo.trandiscore.netty;



import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

public interface RemotingClient {

    Object sendSyncRequest(Object msg, boolean cached, boolean checkNodeExists) throws TimeoutException,InterruptedException;

    Object sendSyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel,Boolean> consumer) throws TimeoutException, InterruptedException;

    @SuppressWarnings("UnusedReturnValue")
    MessageFuture sendAsyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel,Boolean> consumer);

    void sendAsyncResponse(NettyPoolKey key, RpcMessage rpcMessage, Object msg) throws Exception;

    void sendAsyncResponse(NettyChannel channel, RpcMessage rpcMessage, Object msg) throws Exception;

    void clearBalanceCache(String xid,  boolean removeStore);
}
