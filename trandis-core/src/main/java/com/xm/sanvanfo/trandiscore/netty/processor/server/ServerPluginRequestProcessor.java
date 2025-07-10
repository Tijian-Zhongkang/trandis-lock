package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.PluginRequestProcessor;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public class ServerPluginRequestProcessor extends PluginRequestProcessor implements ServerPluginProcessor {
    private final AbstractNettyRemoting server;
    private final String serverId;
    public ServerPluginRequestProcessor(AbstractNettyRemoting server, String serverId,  ExecutorService executorService) {
        super(executorService, server);
        this.server = server;
        this.serverId = serverId;
    }

    @Override
    protected void setPluginParameters(IProcessorPlugin plugin, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        setPluginParameters(plugin, ctx, rpcMessage, server, serverId);
    }
}
