package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RemotingProcessor;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchAbstractResponse;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

abstract class ClientSendResponseProcessor implements RemotingProcessor {

    protected final AbstractNettyRemotingClient client;
    protected final String applicationId;
    protected final String address;
    protected final ExecutorService executorService;


    ClientSendResponseProcessor(AbstractNettyRemotingClient client, String applicationId, String address, ExecutorService executorService) {
        this.client = client;
        this.applicationId = applicationId;
        this.address = address;
        this.executorService = executorService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        ObjectHolder<NettyChannel> channelHolder = new ObjectHolder<>();
        MessageTypeAware response = processToResponse(ctx, rpcMessage, channelHolder);
        if(null != channelHolder.getObj().getChannel() && channelHolder.getObj().getChannel().isActive()) {
            client.sendAsyncResponse(channelHolder.getObj(), rpcMessage, response);
        }
        else {
            client.sendAsyncResponse(channelHolder.getObj().getKey(), rpcMessage, response);
        }
    }

    protected abstract MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                 ObjectHolder<NettyChannel> channelHolder) throws Exception;


    @Override
    public ExecutorService getExecutor() {return executorService;}

    protected void setResponseDefault(BranchAbstractResponse response, String xid, String serverId) {

        response.setServerId(serverId);
        response.setApplicationId(applicationId);
        response.setXid(xid);
        response.setCode(200);
        response.setMsg("");
        response.setClientId(client.getClientId());
    }

}
