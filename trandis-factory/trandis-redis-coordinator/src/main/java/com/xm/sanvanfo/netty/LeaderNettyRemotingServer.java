package com.xm.sanvanfo.netty;

import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.server.IServerProcessorPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.server.ServerPluginRequestProcessor;
import com.xm.sanvanfo.trandiscore.netty.processor.server.ServerPluginResponseProcessor;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.channel.ChannelHandler;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_REQUEST;
import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_RESPONSE;

@SuppressWarnings({"unused"})
public class LeaderNettyRemotingServer extends AbstractNettyRemotingServer {
    private LeaderNettyRemotingServer(NettyServerConfig serverConfig, IServerBootstrapPlugin[] plugins, ChannelHandler... channelHandlers) {
        super(serverConfig, plugins, channelHandlers);
    }

    @Override
    public void start() {
        registerProcessor(TYPE_PLUGIN_REQUEST.getCode(), new ServerPluginRequestProcessor(this, serverId, processExecutor));
        registerProcessor(TYPE_PLUGIN_RESPONSE.getCode(), new ServerPluginResponseProcessor(this, serverId, processExecutor));
        super.start();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private NettyServerConfig serverConfig;
        private IServerBootstrapPlugin[] plugins;
        private ChannelHandler[] channelHandlers;
        private IMessageTimeOutPlugin[] timeOutPlugins;

        public Builder config(NettyServerConfig serverConfig) {
            this.serverConfig = serverConfig;
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

        public Builder registerProcessorPlugin(IServerProcessorPlugin plugin) {
            PluginLoader.INSTANCE().registerPlugin(plugin);
            return this;
        }

        public Builder timeOutPlugins(IMessageTimeOutPlugin[] plugins) {
            timeOutPlugins = plugins;
            return this;
        }

        public Builder plugins(IServerBootstrapPlugin[] plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder handlers(ChannelHandler[] channelHandlers) {
            this.channelHandlers = channelHandlers;
            return this;
        }

        public LeaderNettyRemotingServer build() {
            LeaderNettyRemotingServer server = new LeaderNettyRemotingServer(serverConfig, plugins, channelHandlers);
            if(null != timeOutPlugins) {
                for (IMessageTimeOutPlugin plugin:timeOutPlugins
                     ) {
                    server.addTimeoutPlugin(plugin);
                }
            }
            return server;
        }

    }
}
