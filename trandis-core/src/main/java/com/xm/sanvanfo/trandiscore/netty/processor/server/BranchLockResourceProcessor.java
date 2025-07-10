package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchLockResourceRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.BranchLockResourceResponse;
import com.xm.sanvanfo.trandiscore.session.GlobalContext;
import com.xm.sanvanfo.trandiscore.session.GlobalSession;
import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BranchLockResourceProcessor implements RemotingProcessor {

    protected final AbstractNettyRemotingServer server;
    protected final String serverId;

    public BranchLockResourceProcessor(AbstractNettyRemotingServer server, String serverId) {
        this.server = server;
        this.serverId = serverId;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage)  {

        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof BranchLockResourceRequest);
        BranchLockResourceRequest request = (BranchLockResourceRequest)obj;
        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
        NettyChannel channel = new NettyChannel(key, ctx.channel());
        channel = server.updateChannel(channel);
        GlobalSession session = GlobalContext.INSTANCE().getSession(request.getXid());
        if(null == session) {
            //fast fail
            String msg = String.format("server has not session lock xid:%s", request.getXid());
            log.warn(msg);
            BranchLockResourceResponse response = new BranchLockResourceResponse();
            response.setCode(404);
            response.setMsg(msg);
            server.sendAsyncResponse(rpcMessage, channel, response);
        }
        else {
            session.lockResources(new ReadWriteLockPath(rpcMessage, channel));
        }
    }
}
