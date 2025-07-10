package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.request.GetLockBusRequest;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import io.netty.channel.ChannelHandlerContext;

import static com.xm.sanvanfo.constants.CoordinatorConst.getLockBusRequestPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = getLockBusRequestPlugin)
public class GetLockBusRequestPluginProcessor extends CoordinatorClientProcessorPlugin {

    @Override
    public Class<?> getSerializerType() {
        return GetLockBusRequest.class;
    }

    @Override
    public void process(AbstractNettyRemoting remoting,  ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object body = rpcMessage.getBody();
        Asserts.isTrue(body instanceof PluginRequest);
        PluginRequest request = (PluginRequest)body;
        Object pluginBody = request.getBodyObj();
        Asserts.isTrue(pluginBody instanceof GetLockBusRequest);
        LockCoordinator.INSTANCE().getCurrentRole().directProcess(new CoordinatorMessage(rpcMessage), (GetLockBusRequest)pluginBody);
    }
}
