package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.RemotingProcessor;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.response.MergeMessageResponse;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public class MergeResponseProcessor implements RemotingProcessor {

    private final AbstractNettyRemotingClient remoting;
    private final ExecutorService executorService;
    public MergeResponseProcessor(AbstractNettyRemotingClient remoting, ExecutorService executorService) {
        this.remoting = remoting;
        this.executorService = executorService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object body = rpcMessage.getBody();
        Asserts.isTrue(body instanceof MergeMessageResponse);
        MergeMessageResponse response = (MergeMessageResponse)body;
        int size = response.getIds().size();
        for(int i =0; i<size; i++) {
            RpcMessage message = new RpcMessage();
            message.setId(response.getIds().get(i));
            message.setBody(response.getResults().get(i));
            remoting.processMessageResult(message);
        }
    }

    @Override
    public ExecutorService getExecutor() {return executorService;}
}
