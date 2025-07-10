package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import io.netty.channel.Channel;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

public class NettyKeyedPoolableObjectFactory implements KeyedPoolableObjectFactory<NettyPoolKey, NettyChannel> {

    private final NettyClientBootstrap bootstrap;

    NettyKeyedPoolableObjectFactory(NettyClientBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }


    @Override
    public NettyChannel makeObject(NettyPoolKey key) {
        String address = key.getAddress();
        Channel channel = bootstrap.getNewChannel(NetUtils.toInetSocketAddress(address));
        return new NettyChannel(key, channel);
    }

    @Override
    public void destroyObject(NettyPoolKey key, NettyChannel obj) {
        if(obj.getChannel().isActive()) {
            obj.getChannel().disconnect();
        }
        if(obj.getChannel().isOpen()) {
            obj.getChannel().close();
        }
    }

    @Override
    public boolean validateObject(NettyPoolKey key, NettyChannel obj) {
        return obj != null && obj.getChannel() != null && obj.getChannel().isActive();
    }

    @Override
    public void activateObject(NettyPoolKey key, NettyChannel obj)  {
        if(null == obj || null == obj.getChannel()) {
            throw new BusinessException("channel is null");
        }
        if(obj.getChannel().isActive()) {
            return;
        }
        if(obj.getChannel().isActive()) {
            obj.getChannel().disconnect();
        }
        if(obj.getChannel().isOpen()) {
            obj.getChannel().close();
        }
        String address = key.getAddress();
        obj.getChannel().connect(NetUtils.toInetSocketAddress(address));
    }

    @Override
    public void passivateObject(NettyPoolKey key, NettyChannel obj) {

    }
}
