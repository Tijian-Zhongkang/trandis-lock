package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchAbstractRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchAbstractResponse;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

abstract class BranchAbstractProcessor extends ServerSendResponseProcessor {


    BranchAbstractProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }



    void setResponseDefault(BranchAbstractResponse response, BranchAbstractRequest request) {
        response.setApplicationId(request.getApplicationId());
        response.setServerId(serverId);
        response.setCode(200);
        response.setMsg("");
        response.setClientId(request.getClientId());
    }

    NettyChannel updateAndGetChannel(ChannelHandlerContext ctx, BranchAbstractRequest request) {
        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
        NettyChannel channel = new NettyChannel(key, ctx.channel());
        return server.updateChannel(channel);
    }
}
