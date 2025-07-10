package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.request.GetTransactionInfoRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.GetTransactionInfoResponse;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class GetTransactionInfoProcessor extends ServerSendResponseProcessor {

    public GetTransactionInfoProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                 ObjectHolder<NettyChannel> channelHolder, boolean updateChannel)  {
        GetTransactionInfoResponse response = new GetTransactionInfoResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {

            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof GetTransactionInfoRequest);
            GetTransactionInfoRequest request = (GetTransactionInfoRequest)obj;
            if(updateChannel) {
                NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                        NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
                channel = new NettyChannel(key, ctx.channel());
                channel = server.updateChannel(channel);
            }
            GlobalSession session = GlobalContext.INSTANCE().getSession(request.getXid());
            if(null == session) {
                response.setMsg(String.format("session %s is null", request.getXid()));
                response.setCode(404);
            }
            else {
                response.setCode(200);
                response.setMsg("");
                response.setInfo(session.getTransactionInfo());
                response.setTrunk(session.isTrunk(request.getApplicationId()));
            }
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("GetTransactionInfoProcessor process error");
            log.error("GetTransactionInfoProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        channelHolder.setObj(channel);

        return response;
    }
}
