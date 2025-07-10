package com.xm.sanvanfo.trandiscore.netty.processor;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
public abstract class PluginResponseProcessor  extends PluginRequestProcessor {


    public PluginResponseProcessor(ExecutorService executorService, AbstractNettyRemoting remoting) {
        super(executorService, remoting);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        Object obj = rpcMessage.getBody();
        Asserts.isTrue(obj instanceof PluginResponse);
        PluginResponse response = (PluginResponse) obj;
        IProcessorPlugin plugin = PluginLoader.INSTANCE().load(IProcessorPlugin.class, response.getPluginName());
        response.setObj(plugin.deserialize(response.getObjBytes(), rpcMessage.getCodec()));
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
}
