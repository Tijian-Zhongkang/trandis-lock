package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.HeartBeatRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.roles.IRole;
import com.xm.sanvanfo.roles.Leader;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.server.IServerProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.IPluginMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import com.xm.sanvanfo.trandiscore.serializer.SerializerTypeUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CoordinatorServerProcessorPlugin implements IServerProcessorPlugin, ICoordinatorProcessPlugin {

    @Override
    public void process(AbstractNettyRemoting remoting,  ChannelHandlerContext ctx, RpcMessage rpcMessage)  {
        IRole currentRole = LockCoordinator.INSTANCE().getCurrentRole();
        if(!currentRole.getRole().equals(IRole.RoleType.Leader)) {
            log.debug("role is not leader");
            return;
        }
        Leader leader = (Leader)currentRole;
        Object obj = rpcMessage.getBody();
        if(null == obj) {
            log.warn("process obj is null");
            return;
        }
        if(obj instanceof PluginRequest) {
            PluginRequest request = (PluginRequest)obj;
            Object objPlugin = request.getObj();
            if(null == objPlugin) {
                log.warn("objPlugin is null");
            }
            else {
                leader.updateFollower(getCoordinatorId(objPlugin), ctx);
            }
        }
        else {
            log.debug("process message  type  is no PluginRequest type is {}, content is {}", obj.getClass(), obj.toString());
        }
        LockCoordinator.INSTANCE().process(ctx.channel(), rpcMessage);
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



    @Override
    public void setParameters(AbstractNettyRemoting server, String serverId) {
        LockCoordinator.INSTANCE().setServerInfo(server, serverId);
    }

    @Override
    public NettyPoolKey getNettyPoolKey(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object obj  = rpcMessage.getBody();
        Asserts.isTrue(IPluginMessage.class.isAssignableFrom(obj.getClass()));
        IPluginMessage message = (IPluginMessage)obj;
        Object pluginObj = message.getBodyObj();
        Asserts.isTrue(CoordinatorMessageWare.class.isAssignableFrom(pluginObj.getClass()));
        CoordinatorMessageWare coordinatorMessageWare = (CoordinatorMessageWare)pluginObj;
        return new NettyPoolKey(NettyPoolKey.TransactionRole.NOTRANSACTIONCLI, coordinatorMessageWare.getAppName(),
                NetUtils.toStringAddress(ctx.channel().remoteAddress()), coordinatorMessageWare.getCoordinatorId());
    }

    abstract String getCoordinatorId(Object obj);

}
