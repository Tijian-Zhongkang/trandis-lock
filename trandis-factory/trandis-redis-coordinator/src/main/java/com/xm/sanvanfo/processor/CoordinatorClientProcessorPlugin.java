package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.client.IClientProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import com.xm.sanvanfo.trandiscore.serializer.SerializerTypeUtils;
import io.netty.channel.ChannelHandlerContext;

abstract class CoordinatorClientProcessorPlugin implements IClientProcessorPlugin,ICoordinatorProcessPlugin {

    @Override
    public void setParameters(AbstractNettyRemoting client, String applicationId, String address) {
        LockCoordinator.INSTANCE().setServerInfo(client, String.format("%s:%s", address, applicationId));
    }


    @Override
    public Object deserialize(byte[] bytes, byte codec) throws Exception {
        Serializer serializer = PluginLoader.INSTANCE().load(Serializer.class, SerializerTypeUtils.getByCode(codec));
        return serializer.deserialize(bytes, getSerializerType());
    }

    @Override
    public byte[] serialize(Object obj, byte codec) throws Exception {
        Serializer serializer = PluginLoader.INSTANCE().load(Serializer.class, SerializerTypeUtils.getByCode(codec));
        return serializer.serialize(obj);
    }

}
