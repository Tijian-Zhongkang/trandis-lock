package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.request.TransactionCreateRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.AbstractTransactionResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.TransactionCreateResponse;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionCreateProcessor extends ServerSendResponseProcessor {


    public TransactionCreateProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }

    @Override
    protected AbstractTransactionResponse processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                       ObjectHolder<NettyChannel> channelHolder, boolean updateChannel) {
        TransactionCreateResponse response = new TransactionCreateResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof TransactionCreateRequest);
            TransactionCreateRequest request = (TransactionCreateRequest) obj;
            if(updateChannel) {
                NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                        NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
                channel = new NettyChannel(key, ctx.channel());
                channel = server.updateChannel(channel);
            }
            GlobalContext.INSTANCE().createSession(request.getTransactionInfo());

            response.setCode(200);
            response.setServerId(serverId);
            response.setMsg("");
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("TransactionCreateProcessor process error");
            log.error("TransactionCreateProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        channelHolder.setObj(channel);
        return response;
    }
}
