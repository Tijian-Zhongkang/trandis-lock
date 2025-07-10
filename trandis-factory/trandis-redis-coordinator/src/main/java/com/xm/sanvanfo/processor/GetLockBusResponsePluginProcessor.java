package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.response.GetLockBusResponse;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import io.netty.channel.ChannelHandlerContext;

import static com.xm.sanvanfo.constants.CoordinatorConst.getLockBusResponsePlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = getLockBusResponsePlugin)
public class GetLockBusResponsePluginProcessor extends CoordinatorServerProcessorPlugin {
    @Override
    public Class<?> getSerializerType() {
        return GetLockBusResponse.class;
    }

    @Override
    public void process(AbstractNettyRemoting remoting,  ChannelHandlerContext ctx, RpcMessage rpcMessage)  {
        remoting.processMessageResult(rpcMessage);
    }

    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof GetLockBusResponse);
        return ((GetLockBusResponse)obj).getCoordinatorId();
    }
}
