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
import com.xm.sanvanfo.trandiscore.protocol.request.GetTransactionLocksRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.GetTransactionLocksResponse;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class GetTransactionLocksProcessor extends BranchAbstractProcessor {

    public GetTransactionLocksProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                  ObjectHolder<NettyChannel> channelHolder, boolean updateChannel)  {
        GetTransactionLocksResponse response = new GetTransactionLocksResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof GetTransactionLocksRequest);
            GetTransactionLocksRequest request = (GetTransactionLocksRequest)obj;
            if(updateChannel) {
                channel = updateAndGetChannel(ctx, request);
            }
            setResponseDefault(response, request);
            GlobalSession session = GlobalContext.INSTANCE().getSession(request.getXid());
            if(null == session) {
                response.setCode(404);
                response.setMsg(String.format("xid:%s do not has global session", request.getXid()));
            }
            else {
                response.setLocks(session.getLocks());
            }
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("GetTransactionLocksProcessor process error");
            log.error("GetTransactionLocksProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        channelHolder.setObj(channel);
        return response;
    }
}
