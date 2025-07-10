package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.code.ProtocolDecoder;
import com.xm.sanvanfo.trandiscore.netty.code.ProtocolEncoder;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NettyServerBootstrap implements RemotingBootstrap {

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private final EventLoopGroup eventLoopGroupBoss;
    private final NettyServerConfig nettyServerConfig;
    private ChannelHandler[] channelHandlers;
    private IServerBootstrapPlugin[] plugins;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @SuppressWarnings({"WeakerAccess"})
    public NettyServerBootstrap(NettyServerConfig nettyServerConfig, IServerBootstrapPlugin[] plugins, ChannelHandler... handlers) {

        this.nettyServerConfig = nettyServerConfig;
        this.channelHandlers = handlers;
        this.plugins = plugins;
        if (nettyServerConfig.enableEpoll()) {
            this.eventLoopGroupBoss = new EpollEventLoopGroup(nettyServerConfig.getBossThreadSize(),
                    new DefaultThreadFactory(nettyServerConfig.getBOSS_THREAD_PREFIX(), nettyServerConfig.getBossThreadSize()));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                    new DefaultThreadFactory(nettyServerConfig.getWORKER_THREAD_PREFIX(),
                            nettyServerConfig.getServerWorkerThreads()));
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(nettyServerConfig.getBossThreadSize(),
                    new DefaultThreadFactory(nettyServerConfig.getBOSS_THREAD_PREFIX(), nettyServerConfig.getBossThreadSize()));
            this.eventLoopGroupWorker = new NioEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                    new DefaultThreadFactory(nettyServerConfig.getWORKER_THREAD_PREFIX(),
                            nettyServerConfig.getServerWorkerThreads()));
        }
    }


    @Override
    public void start() {
        Optional.ofNullable(plugins).ifPresent(o->{
            for (IServerBootstrapPlugin plugin:o
                 ) {
                plugin.doConfiguration(nettyServerConfig);
            }
        });
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
                .channel(nettyServerConfig.getSERVER_CHANNEL_CLAZZ())
                .option(ChannelOption.SO_BACKLOG, nettyServerConfig.getSoBackLogSize())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSendBufSize())
                .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketResvBufSize())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(nettyServerConfig.getWriteBufferLowWaterMark(),
                                nettyServerConfig.getWriteBufferHighWaterMark()))
                .localAddress(StringUtils.isEmpty(nettyServerConfig.getBindHost()) ?
                        new InetSocketAddress(nettyServerConfig.getListenPort()) :
                        new InetSocketAddress(nettyServerConfig.getBindHost(), nettyServerConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new IdleStateHandler(nettyServerConfig.getMAX_READ_IDLE_SECONDS(), 0, 0))
                                .addLast(new ProtocolDecoder())
                                .addLast(new ProtocolEncoder());
                        if (channelHandlers != null) {
                            addChannelPipelineLast(ch, channelHandlers);
                        }

                    }
                });

        try {
            Optional.ofNullable(plugins).ifPresent(o->{
                for (IServerBootstrapPlugin plugin:o
                ) {
                    plugin.doBindBefore(serverBootstrap);
                }
            });
            ChannelFuture future = this.serverBootstrap.bind(nettyServerConfig.getListenPort()).sync();
            future.addListener(o->{
                if(!o.isSuccess()) {
                    initialized.set(false);
                    log.warn("bind exception : " + BusinessException.exceptionFullMessage(o.cause()));
                }
            });
            log.info("Server started, listen port: {}", nettyServerConfig.getListenPort());
            Optional.ofNullable(plugins).ifPresent(o->{
                for (IServerBootstrapPlugin plugin:o
                ) {
                    plugin.doBindCompleted(serverBootstrap, nettyServerConfig);
                }
            });
            initialized.set(true);
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    @Override
    public void shutdown() {
        try {
            log.info("shutdown server!");
            if (initialized.get()) {
                Optional.ofNullable(plugins).ifPresent(o -> {
                    for (IServerBootstrapPlugin plugin : o) {
                        plugin.doShutdown(nettyServerConfig);
                    }
                });
                TimeUnit.SECONDS.sleep(nettyServerConfig.getServerShutdownWaitTime());
            }
            eventLoopGroupBoss.shutdownGracefully();
            eventLoopGroupWorker.shutdownGracefully();
        }
        catch (InterruptedException ex) {
            log.error("shutdown error:" + BusinessException.exceptionFullMessage(ex));
        }
    }

    private void addChannelPipelineLast(Channel channel, ChannelHandler... handlers) {
        if (channel != null && handlers != null) {
            channel.pipeline().addLast(handlers);
        }
    }
}
