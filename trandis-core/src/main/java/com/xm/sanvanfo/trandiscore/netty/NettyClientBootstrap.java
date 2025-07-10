package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.code.ProtocolDecoder;
import com.xm.sanvanfo.trandiscore.netty.code.ProtocolEncoder;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.constrant.TransportServerType;
import com.xm.sanvanfo.trandiscore.netty.plugins.IClientBootstrapPlugin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused"})
@Slf4j
public class NettyClientBootstrap implements RemotingBootstrap {

    private final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup defaultEventExecutorGroup;
    private final  NettyPoolKey.TransactionRole transactionRole;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private ChannelHandler[] channelHandlers;
    private IClientBootstrapPlugin[] plugins;

    public NettyClientBootstrap( NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                                 NettyPoolKey.TransactionRole transactionRole, IClientBootstrapPlugin[] plugins,  ChannelHandler ... handlers) {
        this.nettyClientConfig = nettyClientConfig;
        this.defaultEventExecutorGroup = eventExecutorGroup;
        this.transactionRole = transactionRole;
        int selectorThreadSizeThreadSize = this.nettyClientConfig.getClientWorkerThreads();
        this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSizeThreadSize,
                new DefaultThreadFactory(getThreadPrefix(this.nettyClientConfig.getWORKER_THREAD_PREFIX()),
                        selectorThreadSizeThreadSize));

        channelHandlers = handlers;
        this.plugins = plugins;
    }


    @Override
    public void start() {

        Optional.ofNullable(plugins).ifPresent(o->{
            for (IClientBootstrapPlugin plugin:o
                 ) {
                plugin.doConfiguration(nettyClientConfig);
            }
        });

        if (this.defaultEventExecutorGroup == null) {
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                    new DefaultThreadFactory(getThreadPrefix(nettyClientConfig.getWORKER_THREAD_PREFIX()),
                            nettyClientConfig.getClientWorkerThreads()));
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(
                nettyClientConfig.getClientChannelClazz()).option(
                ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(
                ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()).option(ChannelOption.SO_RCVBUF,
                nettyClientConfig.getClientSocketRcvBufSize());

        if (nettyClientConfig.getTRANSPORT_SERVER_TYPE() == TransportServerType.NATIVE) {
            if (PlatformDependent.isOsx()) {
                log.info("client run on macOS");
            } else {
                bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                        .option(EpollChannelOption.TCP_QUICKACK, true);
            }
        }

        bootstrap.handler(
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                                new IdleStateHandler(nettyClientConfig.getMAX_READ_IDLE_SECONDS(),
                                        nettyClientConfig.getMAX_WRITE_IDLE_SECONDS(),
                                        nettyClientConfig.getMAX_ALL_IDLE_SECONDS()))
                                .addLast(new ProtocolDecoder())
                                .addLast(new ProtocolEncoder());
                        if (channelHandlers != null) {
                            addChannelPipelineLast(ch, channelHandlers);
                        }
                    }
                });
        Optional.ofNullable(plugins).ifPresent(o->{
            for (IClientBootstrapPlugin plugin:o
            ) {
                plugin.doStarted(bootstrap);
            }
        });
        if (initialized.compareAndSet(false, true)) {
            log.info("NettyClientBootstrap has started");
        }
    }

    @Override
    public void shutdown() {
        try {
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception exx) {
            log.error("Failed to shutdown: {}", exx.getMessage());
        }
    }

    public Channel getNewChannel(InetSocketAddress address) {
        Channel channel;
        ChannelFuture f = this.bootstrap.connect(address);
        try {
            f.await(this.nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (f.isCancelled()) {
                throw new BusinessException(f.cause(), "connect cancelled, can not connect to services-server.");
            } else if (!f.isSuccess()) {
                throw new BusinessException(f.cause(), "connect failed, can not connect to services-server.");
            } else {
                channel = f.channel();
            }
        } catch (Exception e) {
            throw new BusinessException(e, "can not connect to services-server.");
        }

        Optional.ofNullable(plugins).ifPresent(o->{
            for (IClientBootstrapPlugin plugin:o
            ) {
                plugin.doConnectCompleted(bootstrap, address, channel);
            }
        });
        return channel;
    }

    private void addChannelPipelineLast(Channel channel, ChannelHandler... handlers) {
        if (channel != null && handlers != null) {
            channel.pipeline().addLast(defaultEventExecutorGroup, handlers);
        }
    }

    private String getThreadPrefix(String threadPrefix) {
        return threadPrefix + "_" + transactionRole.name();
    }
}
