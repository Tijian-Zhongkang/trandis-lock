package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.google.common.collect.Lists;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchRollbackRequest;
import com.xm.sanvanfo.trandiscore.protocol.request.ServerToBranchRollbackRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.AbstractTransactionResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchRollbackResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.ServerToBranchRollbackResponse;
import com.xm.sanvanfo.trandiscore.session.BranchSection;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;


@Slf4j
public class BranchRollbackProcessor extends BranchAbstractProcessor {

    public BranchRollbackProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }

    @Override
    protected AbstractTransactionResponse processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                         ObjectHolder<NettyChannel> channelHolder, boolean updateChannel) {
        BranchRollbackResponse response = new BranchRollbackResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof BranchRollbackRequest);
            BranchRollbackRequest request = (BranchRollbackRequest)obj;
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
                response.setXid(session.getTransactionInfo().getXid());
                if (request.getTrunk()) {
                    sendSyncBranchRollback(session);
                    GlobalContext.INSTANCE().closeSession(session.getTransactionInfo().getXid());
                }
            }
        }
        catch (Exception ex) {
            response.setCode(500);
            response.setMsg("BranchRollbackProcessor process error");
            log.error("BranchRollbackProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        if(updateChannel) {
            channelHolder.setObj(channel);
        }
        return  response;
    }

    private void sendSyncBranchRollback(GlobalSession session, List<BranchSection> list) throws Exception {
        ServerToBranchRollbackRequest request = new ServerToBranchRollbackRequest();
        request.setXid(session.getTransactionInfo().getXid());
        request.setServerId(serverId);
        BranchSection last = session.getLastSection();
        if(null != last && !last.getEnd()) {
            return;
        }
        for (BranchSection section:list
        ) {
            request.setSectionId(section.getSectionNumber());
            Object obj = server.sendSyncRequestSampleApplication(section.getClientKey(), request);
            Asserts.isTrue(obj instanceof ServerToBranchRollbackResponse);
            ServerToBranchRollbackResponse response = (ServerToBranchRollbackResponse) obj;
            if (!response.getCode().equals(200)) {
                throw new BusinessException("send ServerToBranchRollbackRequest error msg:" + response.getMsg());
            }
        }
    }

    public void sendSyncBranchRollback(GlobalSession session) throws Exception{
        List<BranchSection> sections = session.getAll();
        List<BranchSection> list = Lists.reverse(sections);
        sendSyncBranchRollback(session, list);
    }
}
