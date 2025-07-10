package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.request.HeartbeatMessageRequest;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.response.HeartbeatMessageResponse;
import io.netty.channel.ChannelHandlerContext;


public class HeartBeatMessageProcessor extends ServerSendResponseProcessor {


    public HeartBeatMessageProcessor(AbstractNettyRemotingServer server, String serverId) {
        super(server, serverId, null);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage, ObjectHolder<NettyChannel> channelHolder,
                                              boolean updateChannel) {

        HeartbeatMessageResponse response = new HeartbeatMessageResponse();
        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof HeartbeatMessageRequest);
        HeartbeatMessageRequest request = (HeartbeatMessageRequest)obj;
        if(updateChannel) {
            NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                    NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
            NettyChannel channel = new NettyChannel(key, ctx.channel());
            server.updateChannel(channel);
            channelHolder.setObj(channel);
        }

        response.setServerId(serverId);
        return response;
    }
}
