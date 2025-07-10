package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import io.netty.channel.ChannelHandlerContext;

import static com.xm.sanvanfo.constants.CoordinatorConst.acquireResponsePlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = acquireResponsePlugin)
public class AcquireResponsePluginProcessor extends CoordinatorClientProcessorPlugin {

    @Override
    public Class<?> getSerializerType() {
        return AcquireResponse.class;
    }


    @Override
    public void  process(AbstractNettyRemoting remoting, ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        super.process(remoting, ctx, rpcMessage);
        Object body = rpcMessage.getBody();
        Asserts.isTrue(body instanceof PluginResponse);
        LockCoordinator.INSTANCE().processResponse((PluginResponse)body);
    }
}
