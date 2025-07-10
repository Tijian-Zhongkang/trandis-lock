package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.protocol.request.NotifyRequest;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import io.netty.channel.ChannelHandlerContext;

import static com.xm.sanvanfo.constants.CoordinatorConst.notifyRequestPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = notifyRequestPlugin)
public class NotifyRequestPluginProcessor extends CoordinatorClientProcessorPlugin {

    @Override
    public Class<?> getSerializerType() {
        return NotifyRequest.class;
    }

    @Override
    public void process(AbstractNettyRemoting remoting, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        LockCoordinator.INSTANCE().process(ctx.channel(), rpcMessage);
    }
}
