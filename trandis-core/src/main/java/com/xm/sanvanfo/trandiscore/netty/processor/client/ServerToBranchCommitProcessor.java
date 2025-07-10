package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.TransactionManager;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.request.ServerToBranchCommitRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.ServerToBranchCommitResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class ServerToBranchCommitProcessor extends ClientSendResponseProcessor {

    public ServerToBranchCommitProcessor(AbstractNettyRemotingClient client, String applicationId, String address, ExecutorService executorService) {
        super(client, applicationId, address, executorService);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage, ObjectHolder<NettyChannel> channelHolder) {
        ServerToBranchCommitResponse response = new ServerToBranchCommitResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof ServerToBranchCommitRequest);
            ServerToBranchCommitRequest request = (ServerToBranchCommitRequest)obj;
            String xid = request.getXid();
            setResponseDefault(response, xid, request.getServerId());
            channel = client.convertTo(ctx.channel());
            TransactionManager.INSTANCE().branchCommit(xid);
        }
        catch (Exception ex) {
           response.setCode(500);
           response.setMsg("ServerToBranchCommitProcessor process error");
           log.error("ServerToBranchCommitProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        channelHolder.setObj(channel);
        return response;
    }
}
