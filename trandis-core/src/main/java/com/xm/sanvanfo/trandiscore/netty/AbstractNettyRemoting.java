package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.config.NettyBaseConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IBootstrapHandlerPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;


@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
public abstract class AbstractNettyRemoting implements Disposable {

    private static final long NOT_WRITEABLE_CHECK_MILLS = 10L;
    protected static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    protected static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;

    protected final ConcurrentHashMap<Integer, MessageFuture> futureMap = new ConcurrentHashMap<>();
    protected final Object writableLock = new Object();
    protected volatile Boolean closed = false;
    protected final Map<Integer, RemotingProcessor> processorMap = new ConcurrentHashMap<>();
    protected final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
    protected NettyPoolKey.TransactionRole transactionRole;

    protected static final int MASK = 0x7FFFFFFF;
    protected final AtomicInteger atom = new AtomicInteger(0);
    protected ExecutorService processExecutor;
    protected final List<IMessageTimeOutPlugin> timeoutPlugin = Collections.synchronizedList(new ArrayList<>());

    public void registerProcessor(Integer messageType, RemotingProcessor processor) {
        processorMap.put(messageType, processor);
    }

    public RemotingProcessor getProcessor(Integer messageType) {
        return processorMap.get(messageType);
    }

    public boolean processMessageResult(RpcMessage result) {
        MessageFuture future = futureMap.remove(result.getId());
        if(null != future) {
            future.setResultMessage(result.getBody());
            return true;
        }
        return false;
    }

    public void addTimeoutPlugin(IMessageTimeOutPlugin timeoutPlugin) {
        this.timeoutPlugin.add(timeoutPlugin);
    }


    public abstract void start();

    @Override
    public void dispose() {
        closed = true;
        hashedWheelTimer.stop();
    }

    public abstract NettyChannel convertTo(Channel channel);

    protected int getNextMessageId() {
        return atom.incrementAndGet() & MASK;
    }

    protected IBootstrapHandlerPlugin[] getDefaultPlugins(IBootstrapHandlerPlugin[] plugins) {
        if(null == plugins) {
            plugins = defaultPlugins();
        }
        else {
            List<IBootstrapHandlerPlugin> list = new ArrayList<>();
            list.addAll(Arrays.asList(plugins));
            list.addAll(Arrays.asList(defaultPlugins()));
            plugins = list.toArray(new IBootstrapHandlerPlugin[]{});
        }
        Arrays.sort(plugins, Comparator.comparingInt(IBootstrapHandlerPlugin::getOrderBy));
        return plugins;
    }

    protected abstract IBootstrapHandlerPlugin[] defaultPlugins();

    protected ChannelHandler[] defaultHandlers(IBootstrapHandlerPlugin[] plugins) {
        return new ChannelHandler[]{new DefaultHandler(plugins)};
    }

    protected RpcMessage buildRequestMessage(Object msg, byte messageType, NettyBaseConfig config) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(getNextMessageId());
        rpcMessage.setMessageType(messageType);
        rpcMessage.setCodec(config.getCONFIGURED_CODEC());
        rpcMessage.setCompressor(config.getCONFIGURED_COMPRESSOR());
        rpcMessage.setBody(msg);
        if(msg instanceof PluginRequest) {
            PluginRequest pluginRequest = (PluginRequest)msg;
            try {
                IProcessorPlugin processorPlugin = PluginLoader.INSTANCE().load(IProcessorPlugin.class, pluginRequest.getPluginName());
                pluginRequest.setObjBytes(processorPlugin.serialize(pluginRequest.getObj(), rpcMessage.getCodec()));
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "plugin request serialize exception");
            }
        }
        if(MessageTypeAware.class.isAssignableFrom(msg.getClass())) {
            rpcMessage.setBodyType(((MessageTypeAware)msg).getMessageType().getCode());
        }
        return rpcMessage;
    }

    protected void channelWritableCheck(NettyChannel channel, Object msg, Integer tryTimesConfig) {
        if(closed) {
            throw  new RuntimeException("remoting is closed");
        }
        if(channel.getChannel().isWritable()) {
            return;
        }
        synchronized (writableLock) {
            int tryTimes = 0;
            while(!channel.getChannel().isWritable()) {
                try {
                    if(tryTimes >= tryTimesConfig || !channel.getChannel().isActive()) {
                        closeChannel(channel);
                        throw new BusinessException(String.format("channel is not writable channel %s, msg:%s",
                                null == channel.getKey() ? "null" : channel.getKey().toString(), msg.toString()));
                    }
                    writableLock.wait(NOT_WRITEABLE_CHECK_MILLS);
                    tryTimes++;
                }
                catch (InterruptedException ex) {
                     throw new BusinessException(ex, "check writable error:" + channel.getKey().toString());
                }
            }
        }
    }

    protected void sendAsyncResponse(NettyChannel channel, RpcMessage message, long timeout, int checkWritableTimes) {
        channelWritableCheck(channel, message.getBody(), checkWritableTimes);
        log.debug("send response:" + message);
        channel.getChannel().writeAndFlush(message).addListener((ChannelFutureListener) future -> {
           if(!future.isSuccess()) {
               closeChannel(convertTo(future.channel()));
           }
        });
    }

    protected void sendAsyncResponse(NettyChannel channel, RpcMessage message, long timeout, int checkWritableTimes,
                                     BiConsumer<NettyChannel, RpcMessage> failConsumer) {
        channelWritableCheck(channel, message.getBody(), checkWritableTimes);
        log.debug("send response:" + message);
        channel.getChannel().writeAndFlush(message).addListener((ChannelFutureListener) future -> {
            if(!future.isSuccess()) {
                closeChannel(convertTo(future.channel()));
                if(null != failConsumer) {
                    failConsumer.accept(channel, message);
                }
            }
        });
    }


    protected MessageFuture sendAsync(NettyChannel channel, RpcMessage message, long timeout, int checkWritableTimes,
                                      BiConsumer<NettyChannel, Boolean> consumer, BiConsumer<NettyChannel, MessageFuture> failConsumer) {

        if(closed) {
            throw new BusinessException("remoting is closed");
        }
        boolean invalidate = false;
        try {
            MessageFuture messageFuture = new MessageFuture();
            messageFuture.setRpcMessage(message);
            messageFuture.setTimeout(hashedWheelTimer, timeout, this::futureTimeout);
            futureMap.put(message.getId(), messageFuture);
            channelWritableCheck(channel, message.getBody(), checkWritableTimes);
            log.debug("send request:" + message);
            channel.getChannel().writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    MessageFuture messageFuture1 = futureMap.remove(message.getId());
                    if (null != messageFuture1) {
                        messageFuture1.setResultException(future.cause());
                    }
                    NettyChannel nettyChannel = convertTo(future.channel());
                    closeChannel(nettyChannel);
                    if(null != failConsumer) {
                        failConsumer.accept(nettyChannel, messageFuture1);
                    }
                }
            });
            return messageFuture;
        }
        catch (Exception ex) {
            invalidate = true;
            MessageFuture messageFuture1 = futureMap.remove(message.getId());
            if (null != messageFuture1) {
                messageFuture1.setResultException(ex);
            }
            throw new BusinessException(ex, ex.getMessage());
        }
        finally {
            if(null != consumer) {
                consumer.accept(channel, invalidate);
            }
        }
    }

    protected void futureTimeout(MessageFuture future) {
        if(closed) {
            return;
        }
        MessageFuture f = futureMap.remove(future.getRpcMessage().getId());
        if(null != f) {
            log.warn("message is timeout:" + future.getRpcMessage().getBody().toString());
            for (IMessageTimeOutPlugin plugin:timeoutPlugin
                 ) {
                plugin.accept(f);
            }
        }
        future.setResultMessage(null);
    }

    protected void processMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        log.debug(String.format("process message id:%d, body:%s", rpcMessage.getId(), rpcMessage.getBody()));
        if(closed) {
            log.debug(String.format("remoting is closed dont process message id:%d, body:%s", rpcMessage.getId(), rpcMessage.getBody()));
            return;
        }
        Object body = rpcMessage.getBody();
        if(body instanceof MessageTypeAware) {
            MessageTypeAware messageTypeAware = (MessageTypeAware) body;
            RemotingProcessor processor = processorMap.get(messageTypeAware.getMessageType().getCode());
            if(processor != null) {
                ExecutorService executorService = processor.getExecutor();
                if (executorService != null) {
                    executorService.execute(() -> {
                        try {
                            processor.process(ctx, rpcMessage);
                        } catch (Exception ex) {
                            log.error(String.format("process message id:%d, body:%s, error stack:%s",
                                    rpcMessage.getId(), rpcMessage.getBody(), BusinessException.exceptionFullMessage(ex)));
                        }
                    });
                } else {
                    try {
                        processor.process(ctx, rpcMessage);
                    } catch (Exception ex) {
                        log.error(String.format("process message id:%d, body:%s, error stack:%s",
                                rpcMessage.getId(), rpcMessage.getBody(), BusinessException.exceptionFullMessage(ex)));
                    }
                }
            }
            else {
                log.error(String.format("the message has no processor type:%d, id:%d, body:%s", rpcMessage.getMessageType(),
                        rpcMessage.getId(), rpcMessage.getBody()));
            }
        }
        else {
            log.error(String.format("the message is error id:%d, body:%s", rpcMessage.getId(), rpcMessage.getBody()));
        }
    }


    protected abstract void closeChannel(NettyChannel channel);

    protected abstract RpcMessage buildRequestMessage(Object msg, byte messageType);

    protected RpcMessage buildResponseMessage(RpcMessage rpcMessage, Object msg, byte messageType) {
        RpcMessage rpcMsg = new RpcMessage();
        rpcMsg.setMessageType(messageType);
        rpcMsg.setCodec(rpcMessage.getCodec()); // same with request
        rpcMsg.setCompressor(rpcMessage.getCompressor());
        rpcMsg.setBody(msg);
        rpcMsg.setId(rpcMessage.getId());
        if(msg instanceof PluginResponse) {
            PluginResponse pluginResponse = (PluginResponse)msg;
            try {
                IProcessorPlugin processorPlugin = PluginLoader.INSTANCE().load(IProcessorPlugin.class, pluginResponse.getPluginName());
                pluginResponse.setObjBytes(processorPlugin.serialize(pluginResponse.getObj(), rpcMessage.getCodec()));
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "plugin request serialize exception");
            }
        }
        if(MessageTypeAware.class.isAssignableFrom(msg.getClass())) {
            rpcMsg.setBodyType(((MessageTypeAware)msg).getMessageType().getCode());
        }
        return rpcMsg;
    }

    @ChannelHandler.Sharable
    class DefaultHandler extends ChannelDuplexHandler {

        private IBootstrapHandlerPlugin[] plugins;

        DefaultHandler(IBootstrapHandlerPlugin[] plugins) {
            this.plugins = plugins;
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if(!(msg instanceof RpcMessage)) {
                return;
            }
            invokePluginMethod(IBootstrapHandlerPlugin.class.getMethod("process", ChannelHandlerContext.class, RpcMessage.class),
                    ctx, msg);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            if(ctx.channel().isWritable()) {
                synchronized (writableLock) {
                    writableLock.notifyAll();
                }
            }
            super.channelWritabilityChanged(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if(closed) {
                return;
            }
            invokePluginMethod(IBootstrapHandlerPlugin.class.getMethod("destroy", ChannelHandlerContext.class), ctx);
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if(evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
                invokePluginMethod(IBootstrapHandlerPlugin.class.getMethod("handleEvent", ChannelHandlerContext.class, IdleStateEvent.class), ctx, idleStateEvent);
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error(String.format("remoting exception address:%s,stack:%s", NetUtils.toStringAddress(ctx.channel().remoteAddress()),
                    BusinessException.exceptionFullMessage(cause)));
            invokePluginMethod(IBootstrapHandlerPlugin.class.getMethod("exception", ChannelHandlerContext.class, Throwable.class), ctx, cause);
            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
            log.info(ctx + "will closed");
            super.close(ctx, future);
        }

        private void invokePluginMethod(Method method, Object... args) {
            Optional.ofNullable(plugins).ifPresent(o->{

                try {
                    for (IBootstrapHandlerPlugin plugin:o
                    ) {
                        if(args == null) {
                            method.invoke(plugin);
                        }
                        else {
                            method.invoke(plugin, args);
                        }
                    }
                }
                catch (Exception ex) {
                    throw new BusinessException(ex, "plugin method invoke error");
                }

            });
        }
    }

}
