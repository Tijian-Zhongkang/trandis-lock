package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.PluginRequestProcessor;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

public class ClientPluginRequestProcessor extends PluginRequestProcessor {

    private final AbstractNettyRemoting client;
    private final String applicationId;
    private final String address;

    public ClientPluginRequestProcessor(AbstractNettyRemoting client, String applicationId, String address,
                                        ExecutorService executorService) {
        super(executorService, client);
        this.client = client;
        this.applicationId = applicationId;
        this.address = address;
    }

    @Override
    protected void setPluginParameters(IProcessorPlugin plugin, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Asserts.isTrue(IClientProcessorPlugin.class.isAssignableFrom(plugin.getClass()));
       IClientProcessorPlugin clientProcessorPlugin = (IClientProcessorPlugin)plugin;
       clientProcessorPlugin.setParameters(client, applicationId, address);
    }
}
