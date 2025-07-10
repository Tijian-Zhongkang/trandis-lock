package com.xm.sanvanfo.netty;

import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.BootstrapServerInfo;
import com.xm.sanvanfo.common.CallBack;
import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.EnvUtils;
import com.xm.sanvanfo.common.utils.RetryUtils;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.roles.Follower;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IBootstrapHandlerPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IClientBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.client.ClientPluginRequestProcessor;
import com.xm.sanvanfo.trandiscore.netty.processor.client.ClientPluginResponseProcessor;
import com.xm.sanvanfo.trandiscore.netty.processor.client.IClientProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_REQUEST;
import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_RESPONSE;

@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
public class FollowerNettyRemotingClient extends AbstractNettyRemoting {

    private final NettyClientBootstrap clientBootstrap;
    private final CoordinatorConfig config;
    private final NettyClientConfig nettyClientConfig;
    private BootstrapServerInfo bootstrapServerInfo = null;
    @Getter
    private NettyChannel nettyChannel;
    private Follower follower;
    private String address;

    public FollowerNettyRemotingClient(NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                                       CoordinatorConfig config, Follower follower) {
        this.config = config;
        this.nettyClientConfig = nettyClientConfig;
        IClientBootstrapPlugin[] plugins = Arrays.stream(getDefaultPlugins(null)).map(o-> (IClientBootstrapPlugin)o)
        .collect(Collectors.toList()).toArray(new IClientBootstrapPlugin[]{});
        clientBootstrap = new NettyClientBootstrap(nettyClientConfig, eventExecutorGroup, NettyPoolKey.TransactionRole.CLIROLE,
                plugins, defaultHandlers(plugins));
        this.follower = follower;

    }

    @Override
    public void start() {
        address = Objects.requireNonNull(EnvUtils.getIpAddress(nettyClientConfig.getNetworkCardName(), nettyClientConfig.getIpType())).getHostAddress();
        registerProcessor(TYPE_PLUGIN_RESPONSE.getCode(), new ClientPluginResponseProcessor(this, config.getAppName(), address,  processExecutor));
        registerProcessor(TYPE_PLUGIN_REQUEST.getCode(), new ClientPluginRequestProcessor(this, config.getAppName(), address,  processExecutor));
        clientBootstrap.start();
    }

    @Override
    public void dispose() {
        super.dispose();
        clientBootstrap.shutdown();
    }

    public void validateConnection(BootstrapServerInfo bootstrapServerInfo, int times, RetryUtils.RetryType type, long millis, Supplier<BootstrapServerInfo> supplier, CallBack callBack) {

        if(StringUtils.isEmpty(bootstrapServerInfo.getIp())) {
            throw new IllegalArgumentException("Bootstrap server has not been initialized");
        }
        RetryUtils.invokeRetryTimes("follower validateConnection", o->{

            if( null == this.bootstrapServerInfo || null == this.bootstrapServerInfo.getIp() ||
                    null == this.bootstrapServerInfo.getPort() ||
                    !this.bootstrapServerInfo.getIp().equals(bootstrapServerInfo.getIp()) ||
                    !this.bootstrapServerInfo.getPort().equals(bootstrapServerInfo.getPort()) ||
                    null == nettyChannel ||null == nettyChannel.getChannel()||
                    !nettyChannel.getChannel().isActive()) {
                try {
                    if (null != nettyChannel && null != nettyChannel.getChannel() && nettyChannel.getChannel().isActive()) {
                        closeChannel(nettyChannel);
                    }
                    InetSocketAddress address = InetSocketAddress.createUnresolved(bootstrapServerInfo.getIp(), bootstrapServerInfo.getPort());
                    Channel channel = clientBootstrap.getNewChannel(address);
                    this.nettyChannel = convertTo(channel);
                    //Must refresh, as there may still be the original IP and port, but the ID has changed
                    this.bootstrapServerInfo = supplier.get();
                    if(null == this.bootstrapServerInfo || null == this.bootstrapServerInfo.getIp() || null == this.bootstrapServerInfo.getPort()
                            || !this.bootstrapServerInfo.getIp().equals(bootstrapServerInfo.getIp())
                            || !this.bootstrapServerInfo.getPort().equals(bootstrapServerInfo.getPort())) {
                        closeChannel(nettyChannel);
                        throw new BusinessException(String.format("refresh connection error bootstrap is %s:",
                                null == this.bootstrapServerInfo ? "null" : this.bootstrapServerInfo.toString()));
                    }
                    sendAsyncHeartBeatRequest();
                } catch (Exception ex) {
                    throw new BusinessException(ex, "validate connection error:" + bootstrapServerInfo.toString());
                }
            }
            if(null != callBack) {
                callBack.apply();
            }
        }, times, type, millis);
    }

    public List<RpcMessage> getAllLockedMessages() {
        return futureMap.values().stream().map(MessageFuture::getRpcMessage).filter(o->{
            if(!(o.getBody() instanceof PluginRequest)) {
                return false;
            }
            PluginRequest request = (PluginRequest)o.getBody();
            Object obj = request.getBodyObj();
            return obj instanceof AcquireRequest || obj instanceof ReleaseRequest;
        }).collect(Collectors.toList());
    }

    public MessageFuture sendAsyncRequest(PluginRequest messageWare, BiConsumer<NettyChannel, MessageFuture> failConsumer) {
        if(null != nettyChannel && null != nettyChannel.getChannel() && nettyChannel.getChannel().isActive()) {
            RpcMessage rpcMessage = buildRequestMessage(messageWare, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
            return sendAsync(this.nettyChannel, rpcMessage, nettyClientConfig.getRPC_REQUEST_TIMEOUT(),
                    nettyClientConfig.getMAX_NOT_WRITEABLE_RETRY(), null, failConsumer);
        }
        throw new BusinessException("netty channel is not active");
    }

    public Object sendSyncRequest(PluginRequest messageWare, BiConsumer<NettyChannel, MessageFuture> failConsumer,
                                  Long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        MessageFuture future = sendAsyncRequest(messageWare, failConsumer);
        return future.get(timeout, timeUnit);
    }

    public Object sendSyncRequest(PluginRequest messageWare, BiConsumer<NettyChannel, MessageFuture> failConsumer)
            throws TimeoutException, InterruptedException {
        MessageFuture future = sendAsyncRequest(messageWare, failConsumer);
        return future.get(nettyClientConfig.getRPC_REQUEST_TIMEOUT(), TimeUnit.MILLISECONDS);
    }

    public void sendAsyncResponse(RpcMessage rpcMessage, PluginResponse msg) {
        RpcMessage rpcMsg = buildResponseMessage(rpcMessage, msg, ProtocolConstants.MSGTYPE_RESPONSE);
        super.sendAsyncResponse(this.nettyChannel, rpcMsg, nettyClientConfig.getRPC_REQUEST_TIMEOUT(), nettyClientConfig.getMAX_NOT_WRITEABLE_RETRY());
    }

    public BootstrapServerInfo getBootstrapServerInfo() {
        return bootstrapServerInfo;
    }


    @Override
    protected IBootstrapHandlerPlugin[] defaultPlugins() {
        return new IBootstrapHandlerPlugin[]{new FollowerClientPlugin()};
    }

    @Override
    public NettyChannel convertTo(Channel channel) {
        NettyPoolKey poolKey = new NettyPoolKey(NettyPoolKey.TransactionRole.NOTRANSACTIONCLI, config.getAppName(),
                address, config.getAppName());
        return new NettyChannel(poolKey, channel);
    }

    @Override
    protected void closeChannel(NettyChannel channel) {
        channel.getChannel().disconnect();
        channel.getChannel().close();
    }

    @Override
    protected RpcMessage buildRequestMessage(Object msg, byte messageType) {
        return buildRequestMessage(msg, messageType, nettyClientConfig);
    }

    private void sendAsyncHeartBeatRequest() {

        PluginRequest heartBeat = follower.createHeartBeat();
        try {
            sendAsyncRequest(heartBeat, null);
        }
        catch (Exception ex) {
            log.warn("send heartbeat error:" + BusinessException.exceptionFullMessage(ex));
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    class FollowerClientPlugin implements IClientBootstrapPlugin {

        @Override
        public void process(ChannelHandlerContext ctx, RpcMessage msg) {
            processMessage(ctx, msg);
        }

        @Override
        public void handleEvent(ChannelHandlerContext ctx, IdleStateEvent event) {

            Channel channel = ctx.channel();
            NettyChannel nettyChannel = convertTo(channel);
            if (event.state().equals(IdleState.READER_IDLE)) {
                closeChannel(nettyChannel);
            }
            if (event.equals(IdleStateEvent.WRITER_IDLE_STATE_EVENT)) {
                sendAsyncHeartBeatRequest();
            }

        }

        @Override
        public void destroy(ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            NettyChannel nettyChannel = convertTo(channel);
            closeChannel(nettyChannel);

        }

        @Override
        public void exception(ChannelHandlerContext ctx, Throwable cause)  {
            destroy(ctx);
        }

        @Override
        public int getOrderBy() {
            return 0;
        }
    }

    public static class Builder {

        private NettyClientConfig clientConfig;
        private EventExecutorGroup eventExecutorGroup;
        private Follower follower;
        private CoordinatorConfig coordinatorConfig;
        private IMessageTimeOutPlugin[] plugins;

        public Builder config(NettyClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        public Builder coordinatorConfig(CoordinatorConfig coordinatorConfig) {
            this.coordinatorConfig = coordinatorConfig;
            return this;
        }

        public Builder messageTimeoutPlugins(IMessageTimeOutPlugin[] plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder eventExecutorGroup(EventExecutorGroup eventExecutorGroup) {
            this.eventExecutorGroup = eventExecutorGroup;
            return this;
        }

        public Builder follower(Follower follower) {
            this.follower = follower;
            return this;
        }

        public Builder registerCompressors(Compressor... compressors) {
            for (Compressor compressor:compressors
            ) {
                Asserts.isTrue(IPlugin.class.isAssignableFrom(compressor.getClass()));
                PluginLoader.INSTANCE().registerPlugin((IPlugin) compressor);
            }
            return this;
        }

        public Builder registerSerializers(Serializer... serializers) {
            for (Serializer serializer:serializers
            ) {
                Asserts.isTrue(IPlugin.class.isAssignableFrom(serializer.getClass()));
                PluginLoader.INSTANCE().registerPlugin((IPlugin)serializer);
            }
            return this;
        }



        public Builder registerProcessorPlugin(IClientProcessorPlugin plugin) {
            PluginLoader.INSTANCE().registerPlugin(plugin);
            return this;
        }

        public  FollowerNettyRemotingClient build() {
            try {
                FollowerNettyRemotingClient client = new FollowerNettyRemotingClient(clientConfig, eventExecutorGroup, coordinatorConfig,
                        follower);
                if(null != plugins) {
                    for (IMessageTimeOutPlugin plugin:plugins
                         ) {
                        client.addTimeoutPlugin(plugin);
                    }
                }
                return client;
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "build client error");
            }
        }
    }
}
