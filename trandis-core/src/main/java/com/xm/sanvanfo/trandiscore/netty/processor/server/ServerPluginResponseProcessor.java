package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.PluginResponseProcessor;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public class ServerPluginResponseProcessor extends PluginResponseProcessor implements ServerPluginProcessor {

    private final String serverId;

    public ServerPluginResponseProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(executorService, server);
        this.serverId = serverId;
    }

    @Override
    protected void setPluginParameters(IProcessorPlugin plugin, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        setPluginParameters(plugin, ctx, rpcMessage, remoting, serverId);
    }
}
