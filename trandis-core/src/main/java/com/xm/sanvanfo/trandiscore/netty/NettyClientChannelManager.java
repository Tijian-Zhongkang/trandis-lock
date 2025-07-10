package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings({"unused"})
@Slf4j
class NettyClientChannelManager {

    private final GenericKeyedObjectPool<NettyPoolKey, NettyChannel> nettyChannelKeyPool;
    private final ConcurrentHashMap<String, NettyPoolKey> channelMap = new ConcurrentHashMap<>();

    NettyClientChannelManager(NettyKeyedPoolableObjectFactory factory, NettyClientConfig config) {
        nettyChannelKeyPool = new GenericKeyedObjectPool<>(factory);
        nettyChannelKeyPool.setConfig(getKeyedObjectPoolConfig(config));
    }

    private GenericKeyedObjectPool.Config getKeyedObjectPoolConfig(NettyClientConfig config) {
        GenericKeyedObjectPool.Config poolConfig = new GenericKeyedObjectPool.Config();
        poolConfig.maxActive = config.getDEFAULT_MAX_POOL_ACTIVE();
        poolConfig.minIdle = config.getDEFAULT_MIN_POOL_IDLE();
        poolConfig.maxWait = config.getMAX_ACQUIRE_CONN_MILLS();
        poolConfig.testOnBorrow = config.isDEFAULT_POOL_TEST_BORROW();
        poolConfig.testOnReturn = config.isDEFAULT_POOL_TEST_RETURN();
        poolConfig.lifo = config.isDEFAULT_POOL_LIFO();
        return poolConfig;
    }

    NettyPoolKey getFromAddress(String address) {
        return channelMap.get(address);
    }

    NettyChannel acquireChannel(NettyPoolKey nettyPoolKey) throws Exception {
        log.debug("get channel" + nettyPoolKey.getAddress());
        long begin = System.currentTimeMillis();
        NettyChannel channel = nettyChannelKeyPool.borrowObject(nettyPoolKey);
        channelMap.put(nettyPoolKey.getAddress(), nettyPoolKey);
        long end = System.currentTimeMillis();
        log.debug("got channel" + channel.getChannel().localAddress() + " get " + (end - begin) + "ms");
        return channel;
    }

    void releaseChannel(NettyChannel nettyChannel, boolean invalidate) throws Exception {
        log.debug("release channel" + nettyChannel.getKey().getAddress());
        nettyChannelKeyPool.returnObject(nettyChannel.getKey(), nettyChannel);
        if(invalidate) {
            nettyChannelKeyPool.invalidateObject(nettyChannel.getKey(), nettyChannel);
        }
    }



    void invalidateChannel(NettyChannel channel) throws Exception {
        nettyChannelKeyPool.invalidateObject(channel.getKey(), channel);
    }

    void removeChannel(NettyPoolKey poolKey) {
        nettyChannelKeyPool.clear(poolKey);
        channelMap.remove(poolKey.getAddress());
    }

    void removeAll() {
        nettyChannelKeyPool.clear();
        channelMap.clear();
    }



}
