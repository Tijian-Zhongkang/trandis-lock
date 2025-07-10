package com.xm.sanvanfo.trandiscore.netty;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

class NettyServerChannelManager {

    private final ConcurrentHashMap<String, NettyPoolKey> channelMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<NettyPoolKey, Channel>> nettyChannelMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StampedLock> lockObjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<NettyPoolKey, Channel>> nettyApplicationChannelMap = new ConcurrentHashMap<>();

     void insertOrUpdateChannel(NettyChannel channel) {
        long stamp = getLock(channel.getKey().getAddress()).writeLock();
        try {
            channelMap.put(channel.getKey().getAddress(), channel.getKey());
            long idStamp = getLock(channel.getKey().getClientId()).writeLock();
            try {
                ConcurrentHashMap<NettyPoolKey, Channel> nettyMap = nettyChannelMap
                        .computeIfAbsent(channel.getKey().getClientId(), o -> new ConcurrentHashMap<>());
                nettyMap.put(channel.getKey(), channel.getChannel());
            }
            finally {
                getLock(channel.getKey().getClientId()).unlockWrite(idStamp);
            }
            long appStamp = getLock(channel.getKey().getApplicationId()).writeLock();
            try {
                ConcurrentHashMap<NettyPoolKey, Channel> appNettyMap = nettyApplicationChannelMap.computeIfAbsent(channel.getKey().getApplicationId(),
                        o-> new ConcurrentHashMap<>());
                appNettyMap.put(channel.getKey(), channel.getChannel());
            }
            finally {
                getLock(channel.getKey().getApplicationId()).unlockWrite(appStamp);
            }
        }
        finally {
            getLock(channel.getKey().getAddress()).unlockWrite(stamp);
        }
    }

     void releaseChannel(NettyChannel channel) {
         if(null == channel.getKey()) {
             return;
         }
        long stamp = getLock(channel.getKey().getAddress()).writeLock();
        try {
            channelMap.remove(channel.getKey().getAddress());
            long idStamp = getLock(channel.getKey().getClientId()).writeLock();
            try {
                ConcurrentHashMap<NettyPoolKey, Channel> nettyMap = nettyChannelMap.get(channel.getKey().getClientId());
                if (null != nettyMap) {
                    nettyMap.remove(channel.getKey());
                }
            }
            finally {
                getLock(channel.getKey().getClientId()).unlockWrite(idStamp);
            }

            long appStamp = getLock(channel.getKey().getApplicationId()).writeLock();
            try {
                ConcurrentHashMap<NettyPoolKey, Channel> appNettyMap = nettyApplicationChannelMap.get(channel.getKey().getApplicationId());
                if(null != appNettyMap) {
                    appNettyMap.remove(channel.getKey());
                }
            }
            finally {
                getLock(channel.getKey().getApplicationId()).unlockWrite(appStamp);
            }
        }
        finally {
            getLock(channel.getKey().getAddress()).unlockWrite(stamp);
            lockObjects.remove(channel.getKey().getAddress());
        }
    }

     Channel getChannel(NettyPoolKey key) {
        long stamp = getLock(key.getClientId()).readLock();
        try {
            if(!nettyChannelMap.containsKey(key.getClientId())) {
                return null;
            }
            ConcurrentHashMap<NettyPoolKey, Channel> channelMap = nettyChannelMap.get(key.getClientId());
            Channel channel = channelMap.get(key);
            if(null != channel && channel.isActive()) {
                return channel;
            }
            for (Map.Entry<NettyPoolKey, Channel> entry:channelMap.entrySet()
            ) {
                if(!entry.getKey().equals(key)) {
                    return entry.getValue();
                }
            }
            return null;

        }
        finally {
            getLock(key.getClientId()).unlockRead(stamp);
        }
    }



     NettyChannel getSampleChannel(NettyChannel nettyChannel) {
        if(nettyChannel.getChannel().isActive()) {
            return nettyChannel;
        }
        if(null == nettyChannel.getKey() || null == nettyChannel.getKey().getClientId()) {
            return null;
        }
        long stamp = getLock(nettyChannel.getKey().getClientId()).readLock();
        try {
            ConcurrentHashMap<NettyPoolKey, Channel> channelMap = nettyChannelMap.get(nettyChannel.getKey().getClientId());
            if(null == channelMap || 0 == channelMap.size()) {
                return null;
            }
            for (Map.Entry<NettyPoolKey, Channel> entry:channelMap.entrySet()
                 ) {
                if(!entry.getKey().equals(nettyChannel.getKey())) {
                    return new NettyChannel(entry.getKey(), entry.getValue());
                }
            }
            return null;
        }
        finally {
            getLock(nettyChannel.getKey().getClientId()).unlockRead(stamp);
        }
    }

    NettyChannel getSampleApplicationChannel(NettyPoolKey key) {
        Channel channel = getChannel(key);
        if(null != channel) {
            return new NettyChannel(key, channel);
        }
        long appStamp = getLock(key.getApplicationId()).readLock();
        try {
            ConcurrentHashMap<NettyPoolKey, Channel> appNettyMap = nettyApplicationChannelMap.get(key.getApplicationId());
            for (Map.Entry<NettyPoolKey, Channel> entry:appNettyMap.entrySet()
                 ) {
                if(!entry.getKey().equals(key)) {
                    return new NettyChannel(entry.getKey(), entry.getValue());
                }
            }
            return null;
        }
        finally {
            getLock(key.getApplicationId()).unlockRead(appStamp);
        }
    }

     NettyPoolKey getFromAddress(String address) {
        long stamp = getLock(address).readLock();
        try {
            return channelMap.get(address);
        }
        finally {
            getLock(address).unlockRead(stamp);
        }
    }

    private  StampedLock getLock(String key) {
        return lockObjects.computeIfAbsent(key, o -> new StampedLock());
    }

}
