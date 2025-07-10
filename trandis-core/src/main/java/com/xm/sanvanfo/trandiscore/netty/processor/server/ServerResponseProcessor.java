package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.netty.processor.ResponseProcessor;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchAbstractResponse;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public class ServerResponseProcessor extends ResponseProcessor {
    public ServerResponseProcessor(AbstractNettyRemotingServer remoting) {
        super(remoting, null);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        Object obj = rpcMessage.getBody();
        if(BranchAbstractResponse.class.isAssignableFrom(obj.getClass())) {
            BranchAbstractResponse response = (BranchAbstractResponse)obj;
            NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, response.getApplicationId(),
                    NetUtils.toStringAddress(ctx.channel().remoteAddress()), response.getClientId());
            NettyChannel channel = new NettyChannel(key, ctx.channel());
            Asserts.isTrue(AbstractNettyRemotingServer.class.isAssignableFrom(remoting.getClass()));
            ((AbstractNettyRemotingServer)remoting).updateChannel(channel);
        }
        super.process(ctx, rpcMessage);
    }
}
