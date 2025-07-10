package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.TransactionManager;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.request.ServerToBranchSectionNumberRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.ServerToBranchSectionNumberResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class ServerToBranchSectionNumberProcessor extends ClientSendResponseProcessor {

    public ServerToBranchSectionNumberProcessor(AbstractNettyRemotingClient client, String applicationId, String address, ExecutorService executorService) {
        super(client, applicationId, address, executorService);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage, ObjectHolder<NettyChannel> channelHolder)  {
        ServerToBranchSectionNumberResponse response = new ServerToBranchSectionNumberResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof ServerToBranchSectionNumberRequest);
            ServerToBranchSectionNumberRequest request = (ServerToBranchSectionNumberRequest)obj;
            String xid = request.getXid();
            setResponseDefault(response, xid, request.getServerId());
            channel = client.convertTo(ctx.channel());
            TransactionManager.INSTANCE().setSectionNumber(xid, request.getSectionId());
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("ServerToBranchSectionNumberProcessor process error");
            log.error("ServerToBranchSectionNumberProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        channelHolder.setObj(channel);
        return response;
    }
}
