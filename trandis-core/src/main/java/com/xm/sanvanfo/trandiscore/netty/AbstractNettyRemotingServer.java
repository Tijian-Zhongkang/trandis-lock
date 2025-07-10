package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.common.utils.EnvUtils;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.balancer.RegisterProviderFactory;
import com.xm.sanvanfo.trandiscore.netty.balancer.registry.Registry;
import com.xm.sanvanfo.trandiscore.netty.balancer.registry.RegistryProvider;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IBootstrapHandlerPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractNettyRemotingServer extends AbstractNettyRemoting implements RemotingServer {

    private final NettyServerBootstrap serverBootstrap;
    private final NettyServerChannelManager serverChannelManager;
    private final NettyServerConfig serverConfig;
    protected ExecutorService commitRollbackExecutor;
    @Getter
    protected final String serverId;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageServer";
    private static final String COMMIT_ROLLBACK_THREAD_PREFIX = "commitRollbackServer";

    public AbstractNettyRemotingServer(NettyServerConfig serverConfig, IServerBootstrapPlugin[] plugins, ChannelHandler... channelHandlers) {
        this.serverConfig = serverConfig;
        this.transactionRole = NettyPoolKey.TransactionRole.SERVERROLE;
        if(null == channelHandlers || channelHandlers.length == 0) {
            IBootstrapHandlerPlugin[] bootstrapHandlerPlugins = getDefaultPlugins(null == plugins ? null :
                    Arrays.stream(plugins).map(o->(IBootstrapHandlerPlugin)o).collect(Collectors.toList()).toArray(new IBootstrapHandlerPlugin[]{}));
            channelHandlers = defaultHandlers(bootstrapHandlerPlugins);
            plugins = Arrays.stream(bootstrapHandlerPlugins).map(o->(IServerBootstrapPlugin)o).collect(Collectors.toList())
                    .toArray(new IServerBootstrapPlugin[]{});
        }
        serverBootstrap = new NettyServerBootstrap(serverConfig, plugins, channelHandlers);
        serverChannelManager = new NettyServerChannelManager();
        serverId = Objects.requireNonNull(EnvUtils.getIpAddress(serverConfig.getNetworkCardName(), serverConfig.getIpType())).getHostAddress()
                + ":" + serverConfig.getServerAppName();

        processExecutor = new ThreadPoolExecutor(serverConfig.getDEFAULT_MAX_POOL_ACTIVE(), serverConfig.getDEFAULT_MAX_POOL_ACTIVE(), KEEP_ALIVE_TIME,
                TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>(),
                new DefaultThreadFactory(getThreadPrefix(), serverConfig.getDEFAULT_MAX_POOL_ACTIVE()));

        commitRollbackExecutor = new ThreadPoolExecutor(serverConfig.getDEFAULT_MAX_POOL_ACTIVE(), serverConfig.getDEFAULT_MAX_POOL_ACTIVE(), KEEP_ALIVE_TIME,
                TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>(),
                new DefaultThreadFactory(getCommitThreadPrefix(), serverConfig.getDEFAULT_MAX_POOL_ACTIVE()));
    }



    @Override
    public Object sendSyncRequest(NettyPoolKey key, Object msg) throws TimeoutException, InterruptedException {
        Channel channel = serverChannelManager.getChannel(key);
        if(null == channel) {
            log.warn(String.format("channel is null client id:%s", key.getClientId()));
            throw new BusinessException("channel is null");
        }
        return sendSyncRequest(new NettyChannel(key, channel), msg);
    }

    @Override
    public Object sendSyncRequestSampleApplication(NettyPoolKey key, Object msg) throws TimeoutException, InterruptedException {
        NettyChannel channel = serverChannelManager.getSampleApplicationChannel(key);
        if(null == channel) {
            log.warn(String.format("channel is null application id:%s", key.getApplicationId()));
            throw new BusinessException("channel is null");
        }
        return sendSyncRequest(channel, msg);
    }

    @Override
    public Object sendSyncRequest(NettyChannel channel, Object msg) throws TimeoutException, InterruptedException {
        RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        MessageFuture messageFuture = super.sendAsync(channel, message, serverConfig.getRPC_REQUEST_TIMEOUT(),
                serverConfig.getMAX_NOT_WRITEABLE_RETRY(), null, null);
        return messageFuture.get(serverConfig.getRPC_REQUEST_TIMEOUT(), TimeUnit.MILLISECONDS);
    }


    @Override
    public MessageFuture sendAsyncRequest(NettyChannel channel, Object msg) {
        RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        return super.sendAsync(channel, message, serverConfig.getRPC_REQUEST_TIMEOUT(),
                serverConfig.getMAX_NOT_WRITEABLE_RETRY(), null, null);
    }

    @Override
    public MessageFuture sendAsyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel, MessageFuture> failConsumer) {
        RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        return super.sendAsync(channel, message, serverConfig.getRPC_REQUEST_TIMEOUT(),
                serverConfig.getMAX_NOT_WRITEABLE_RETRY(), null, failConsumer);
    }

    @Override
    public void sendAsyncResponse(RpcMessage rpcMessage, NettyChannel channel, Object msg) {
        sendAsyncResponse(rpcMessage,channel, msg, null);
    }

    @Override
    public void sendAsyncResponse(RpcMessage rpcMessage, NettyChannel channel, Object msg, BiConsumer<NettyChannel, NettyResponse> failConsumer) {
        RpcMessage message = buildResponseMessage(rpcMessage, msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        NettyChannel nettyChannel = serverChannelManager.getSampleChannel(channel);
        if(null == nettyChannel) {
            log.warn(String.format("channel is null client id:%s", channel.getKey().getClientId()));
            throw new BusinessException("channel is null");
        }
        super.sendAsyncResponse(nettyChannel, message, serverConfig.getRPC_REQUEST_TIMEOUT(),
                serverConfig.getMAX_NOT_WRITEABLE_RETRY(), (u,v)-> failConsumer.accept(channel, new NettyResponse(rpcMessage, msg)));
    }

    @Override
    public void start() {
        serverBootstrap.start();
    }

    @Override
    public void dispose() {
        serverBootstrap.shutdown();
        super.dispose();
    }

    public NettyChannel updateChannel(NettyChannel nettyChannel) {
        Channel channel = serverChannelManager.getChannel(nettyChannel.getKey());
        if(null == channel || !channel.isActive() ||
                !channel.id().asLongText().equals(nettyChannel.getChannel().id().asLongText())) {
            serverChannelManager.insertOrUpdateChannel(nettyChannel);
            return nettyChannel;
        }
        return convertTo(channel);
    }


    @Override
    public NettyChannel convertTo(Channel channel) {
        NettyPoolKey key = serverChannelManager.getFromAddress(NetUtils.toStringAddress(channel.remoteAddress()));
        return new NettyChannel(key, channel);
    }

    @Override
    protected void closeChannel(NettyChannel channel) {
        log.debug("close socket key is {}", null != channel.getKey() ? channel.getKey().toString() : channel.getChannel().remoteAddress().toString());
        channel.getChannel().close();
        serverChannelManager.releaseChannel(channel);
    }

    @Override
    protected RpcMessage buildRequestMessage(Object msg, byte messageType) {
        return buildRequestMessage(msg, messageType, serverConfig);
    }

    @Override
    protected IServerBootstrapPlugin[] defaultPlugins() {
        return new IServerBootstrapPlugin[] {new DefaultServerPlugin()};
    }

    private String getThreadPrefix() {
        return AbstractNettyRemotingServer.MERGE_THREAD_PREFIX + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }

    private String getCommitThreadPrefix() {
        return  AbstractNettyRemotingServer.COMMIT_ROLLBACK_THREAD_PREFIX + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }

    public class DefaultTrandisServerPlugin extends DefaultServerPlugin{
        @Override
        public void doBindCompleted(ServerBootstrap bootstrap, NettyServerConfig config) {

            try {
                RegistryProvider provider = RegisterProviderFactory.INSTANCE().loadProvider(config.getRegistryType());
                Registry registry = provider.provide();
                registry.register(new InetSocketAddress(Objects.requireNonNull(EnvUtils.getIpAddress(config.getNetworkCardName(),
                        config.getIpType())).getHostAddress(),
                        config.getListenPort()), config.getServerAppName());
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "bind complete error");
            }
        }

        @Override
        public void doShutdown(NettyServerConfig config) {
            try {
                RegistryProvider provider = RegisterProviderFactory.INSTANCE().loadProvider(config.getRegistryType());
                Registry registry = provider.provide();
                registry.unregister(new InetSocketAddress(Objects.requireNonNull(EnvUtils.getIpAddress(config.getNetworkCardName(),
                        config.getIpType())).getHostAddress(),
                        config.getListenPort()), config.getServerAppName());
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "bind complete error");
            }
        }
    }

    private class DefaultServerPlugin implements IServerBootstrapPlugin {

        @Override
        public void process(ChannelHandlerContext ctx, RpcMessage msg)  {
            processMessage(ctx, msg);
        }

        @Override
        public void destroy(ChannelHandlerContext ctx)  {
            Channel channel = ctx.channel();
            NettyChannel nettyChannel = convertTo(channel);
            closeChannel(nettyChannel);
        }

        @Override
        public void exception(ChannelHandlerContext ctx, Throwable cause)  {
            destroy(ctx);
        }

        @Override
        public void handleEvent(ChannelHandlerContext ctx, IdleStateEvent event)  {
            Channel channel = ctx.channel();
            NettyChannel nettyChannel = convertTo(channel);
            if (event.state().equals(IdleState.READER_IDLE)) {
                closeChannel(nettyChannel);
            }
        }

        @Override
        public int getOrderBy() {
            return 0;
        }


    }
}
