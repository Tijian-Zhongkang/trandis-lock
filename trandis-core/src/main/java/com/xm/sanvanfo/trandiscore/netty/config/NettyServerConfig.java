package com.xm.sanvanfo.trandiscore.netty.config;

import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NettyServerConfig extends NettyBaseConfig {

    private int serverSelectorThreads;
    private int serverSocketSendBufSize;
    private int serverSocketResvBufSize;
    private int serverWorkerThreads;
    private int soBackLogSize;
    private int writeBufferHighWaterMark;
    private int writeBufferLowWaterMark;
    private int bossThreadSize;
    private int listenPort;
    private String bindHost;
    private int DEFAULT_LISTEN_PORT = 8091;
    private int RPC_REQUEST_TIMEOUT = 30 * 1000;
    private final String EPOLL_WORKER_THREAD_PREFIX = "NettyServerEPollWorker";
    private int minServerPoolSize;
    private int maxServerPoolSize;
    private int maxTaskQueueSize;
    private int keepAliveTime;
    private int serverShutdownWaitTime;
    private String registryType;
    private String serverAppName;
    private  int DEFAULT_MAX_POOL_ACTIVE = 3;


    /**
     * The Server channel clazz.
     */
    private Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ;


    public boolean enableEpoll() {
        return SERVER_CHANNEL_CLAZZ.equals(EpollServerSocketChannel.class)
                && Epoll.isAvailable();

    }

}
