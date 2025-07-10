package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchBeginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.AbstractTransactionResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchBeginResponse;
import com.xm.sanvanfo.trandiscore.session.BranchSection;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public class BranchBeginProcessor extends BranchAbstractProcessor {

    public BranchBeginProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }


    @Override
    protected AbstractTransactionResponse processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                         ObjectHolder<NettyChannel> channelHolder, boolean updateChannel)  {
        BranchBeginResponse response = new BranchBeginResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof BranchBeginRequest);
            BranchBeginRequest request = (BranchBeginRequest) obj;
            if(updateChannel) {
                channel = updateAndGetChannel(ctx, request);
            }
            GlobalSession session = GlobalContext.INSTANCE().getSession(request.getXid());

            setResponseDefault(response, request);
            if (null == session) {
                response.setCode(404);
                response.setMsg(String.format("xid:%s do not has global session", request.getXid()));
            } else {
                response.setXid(session.getTransactionInfo().getXid());
                BranchSection section = new BranchSection();
                section.setXid(request.getXid());
                section.setClientKey(channel.getKey());
                section.setEnd(false);
                int sectionId = session.add(section);
                response.setSectionNumber(sectionId);
                response.setClientId(request.getClientId());

            }
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("BranchBeginProcessor process error");
            log.error("BranchBeginProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        if(updateChannel) {
            channelHolder.setObj(channel);
        }
        return response;
    }
}
