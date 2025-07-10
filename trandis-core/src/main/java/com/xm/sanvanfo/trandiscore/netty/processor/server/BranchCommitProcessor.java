package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.common.utils.RetryUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.constant.TransactionConst;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchCommitRequest;
import com.xm.sanvanfo.trandiscore.protocol.request.ServerToBranchCommitRequest;
import com.xm.sanvanfo.trandiscore.protocol.request.ServerToBranchSectionNumberRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.AbstractTransactionResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchCommitResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.ServerToBranchCommitResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.ServerToBranchSectionNumberResponse;
import com.xm.sanvanfo.trandiscore.session.BranchSection;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import io.netty.channel.ChannelHandlerContext;
import jdk.nashorn.internal.objects.Global;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class BranchCommitProcessor extends BranchAbstractProcessor {

    public BranchCommitProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService service) {
        super(server, serverId, service);
    }

    @Override
    protected AbstractTransactionResponse processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage,
                                                         ObjectHolder<NettyChannel> channelHolder, boolean updateChannel) {

        BranchCommitResponse response = new BranchCommitResponse();
        NettyChannel channel = new NettyChannel(null, ctx.channel());
        try {
            Object obj = rpcMessage.getBody();
            Asserts.isTrue(obj instanceof BranchCommitRequest);
            BranchCommitRequest request = (BranchCommitRequest) obj;
            setResponseDefault(response, request);
            if(updateChannel) {
                channel = updateAndGetChannel(ctx, request);
            }

            GlobalSession session = GlobalContext.INSTANCE().getSession(request.getXid());
            if(null == session) {
                response.setCode(404);
                response.setMsg(String.format("xid:%s do not has global session", request.getXid()));

            }
            else {
                response.setXid(session.getTransactionInfo().getXid());
                if(request.getTrunk()) {
                    //send branch commit sync
                    sendSyncBranchCommit(session);
                }
                else {
                    BranchSection section =  session.setSectionEnd(request.getXid(), channel.getKey(), request.getSectionNumber());
                    if(null != section) {
                        //send section number to rm
                        sendSyncSectionNumber(session, section.getClientKey(), section.getSectionNumber(),request.getXid());
                    }
                }
            }
        }
        catch (Exception ex) {
             response.setCode(500);
             response.setMsg("BranchCommitProcessor process error");
             log.error(String.format("BranchCommitProcessor process error:%s", BusinessException.exceptionFullMessage(ex)));
        }
        if(updateChannel) {
            channelHolder.setObj(channel);
        }
        return response;
    }

    private void sendSyncSectionNumber(GlobalSession session, NettyPoolKey clientKey, Integer sectionNumber, String xid) {

        ServerToBranchSectionNumberRequest request = new ServerToBranchSectionNumberRequest();
        request.setSectionId(sectionNumber);
        request.setXid(xid);
        request.setServerId(serverId);
        RetryUtils.invokeRetryTimes("sendSyncSectionNumber", o-> {
                    try {
                        Object obj = server.sendSyncRequest(clientKey, request);
                        Asserts.isTrue(obj instanceof ServerToBranchSectionNumberResponse);
                        ServerToBranchSectionNumberResponse response = (ServerToBranchSectionNumberResponse) obj;
                        if (!response.getCode().equals(200)) {
                            throw new BusinessException("send ServerToBranchSectionNumberRequest error msg:" + response.getMsg());
                        }
                    }
                    catch (Exception ex) {
                        throw new BusinessException(ex, ex.getMessage());
                    }
                }, session.getTransactionInfo().getRetryTimes(),
                RetryUtils.RetryType.EXP, TransactionConst.waitMillis);

    }


    private void sendSyncBranchCommit(GlobalSession session) throws Exception {
        Long begin = System.currentTimeMillis();
        List<NettyPoolKey> clients = session.getCommitClients();
        ServerToBranchCommitRequest commitRequest = new ServerToBranchCommitRequest();
        commitRequest.setXid(session.getTransactionInfo().getXid());
        commitRequest.setServerId(serverId);
        for (NettyPoolKey key:clients
             ) {
                    Object obj = server.sendSyncRequest(key, commitRequest);
                    Asserts.isTrue(obj instanceof ServerToBranchCommitResponse);
                    ServerToBranchCommitResponse response = (ServerToBranchCommitResponse) obj;
                    if (!response.getCode().equals(200)) {
                        throw new BusinessException("send ServerToBranchCommitRequest error msg:" + response.getMsg());
                    }
        }
        GlobalContext.INSTANCE().closeSession(session.getTransactionInfo().getXid());
        Long end = System.currentTimeMillis();
        log.debug(String.format("-------%dms xid:%s commit process", end - begin, session.getTransactionInfo().getXid()));
    }
}
