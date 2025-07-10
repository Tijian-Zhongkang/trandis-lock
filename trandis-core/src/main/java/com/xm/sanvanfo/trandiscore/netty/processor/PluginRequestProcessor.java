package com.xm.sanvanfo.trandiscore.netty.processor;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RemotingProcessor;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public abstract class PluginRequestProcessor implements RemotingProcessor {

    protected final ExecutorService service;
    protected final AbstractNettyRemoting remoting;

    public PluginRequestProcessor(ExecutorService executorService, AbstractNettyRemoting remoting) {
        service = executorService;
        this.remoting = remoting;
    }


    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
         Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof PluginRequest);
         PluginRequest request = (PluginRequest)obj;
        IProcessorPlugin plugin = PluginLoader.INSTANCE().load(IProcessorPlugin.class, request.getPlugin());
        request.setObj(plugin.deserialize(request.getObjBytes(), rpcMessage.getCodec()));
        setPluginParameters(plugin, ctx, rpcMessage);
        if(null == plugin.getExecutor()) {
            plugin.process(remoting, ctx, rpcMessage);
        }
        else {
            plugin.getExecutor().execute(()-> {
                try {
                    plugin.process(remoting, ctx, rpcMessage);
                }
                catch (Exception ex) {
                    log.warn(String.format("process message id:%d, body:%s, error stack:%s",
                            rpcMessage.getId(), rpcMessage.getBody(), BusinessException.exceptionFullMessage(ex)));
                }
            });
        }
    }


    @Override
    public ExecutorService getExecutor() {
        return service;
    }

    protected abstract void setPluginParameters(IProcessorPlugin plugin, ChannelHandlerContext ctx, RpcMessage rpcMessage);
}
