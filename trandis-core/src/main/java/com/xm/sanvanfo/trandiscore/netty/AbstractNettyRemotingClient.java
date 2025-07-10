package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.balancer.Balancer;
import com.xm.sanvanfo.trandiscore.netty.balancer.LoadBalanceFactory;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.InvokerListener;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IBootstrapHandlerPlugin;
import com.xm.sanvanfo.trandiscore.netty.plugins.IClientBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.HeartbeatMessageRequest;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchAbstractRequest;
import com.xm.sanvanfo.trandiscore.protocol.request.MergeMessageRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings({"WeakerAccess"})
public abstract class AbstractNettyRemotingClient extends AbstractNettyRemoting implements RemotingClient, InvokerListener {


    private static final int MAX_MERGE_SEND_MILLS = 1000;

    private static final int MAX_MERGE_SEND_THREAD = 1;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageClient";

    protected ExecutorService mergeSendExecutorService;
    protected final NettyClientConfig clientConfig;
    protected final NettyClientBootstrap clientBootstrap;
    protected final NettyClientChannelManager clientChannelManager;

    protected final Object mergeLock = new Object();
    protected final ConcurrentHashMap<NettyPoolKey, BlockingDeque<RpcMessage>> basketMap = new ConcurrentHashMap<>();
    protected Set<SocketAddress> servers;
    protected String address;
    protected volatile boolean isSending = false;

    public AbstractNettyRemotingClient(NettyClientConfig clientConfig, EventExecutorGroup eventExecutorGroup,
                                       IClientBootstrapPlugin[] plugins, ChannelHandler... handlers) {
        this.clientConfig = clientConfig;
        this.transactionRole = NettyPoolKey.TransactionRole.CLIROLE;
        if(null == handlers || handlers.length == 0) {
            IBootstrapHandlerPlugin[] bootstrapHandlerPlugins = getDefaultPlugins(null == plugins ? null :
                    Arrays.stream(plugins).map(o->(IBootstrapHandlerPlugin)o).collect(Collectors.toList()).toArray(new IBootstrapHandlerPlugin[]{}));
            handlers = defaultHandlers(bootstrapHandlerPlugins);
            plugins = Arrays.stream(bootstrapHandlerPlugins).map(o->(IClientBootstrapPlugin)o).collect(Collectors.toList())
                    .toArray(new IClientBootstrapPlugin[]{});
        }

        clientBootstrap = new NettyClientBootstrap(clientConfig, eventExecutorGroup, transactionRole,plugins, handlers);
        clientChannelManager = new NettyClientChannelManager(new NettyKeyedPoolableObjectFactory(clientBootstrap), clientConfig);
        processExecutor = new ThreadPoolExecutor(clientConfig.getDEFAULT_MAX_POOL_ACTIVE(), clientConfig.getDEFAULT_MAX_POOL_ACTIVE(), KEEP_ALIVE_TIME,
                TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>(),
                new DefaultThreadFactory(getThreadPrefix()));
    }


    @Override
    public void clearBalanceCache(String xid, boolean removeStore) {
        try {
            Balancer balancer = LoadBalanceFactory.INSTANCE().loadBalancer(clientConfig.getBalanceType());
            balancer.clearCache(xid, removeStore);
        }
        catch (Exception ex) {
            throw new BusinessException(ex, "clear balance cache error");
        }
    }


    @Override
    public Object sendSyncRequest(Object msg, boolean cached, boolean checkNodeExist) throws TimeoutException, InterruptedException {
        try {
            Balancer balancer = LoadBalanceFactory.INSTANCE().loadBalancer(clientConfig.getBalanceType());
            SocketAddress address = balancer.select(getClusterName(), getXid(msg), cached, checkNodeExist);
            NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, getClusterName(), NetUtils.toStringAddress(address), getClientId());
            if(clientConfig.isENABLE_CLIENT_BATCH_SEND_REQUEST()) {

                RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
               MessageFuture messageFuture = new MessageFuture();
               messageFuture.setRpcMessage(message);
               messageFuture.setTimeout(hashedWheelTimer, clientConfig.getRPC_REQUEST_TIMEOUT(), super::futureTimeout);
                futureMap.put(message.getId(), messageFuture);
               BlockingDeque<RpcMessage> basket = basketMap.computeIfAbsent(key, o->new LinkedBlockingDeque<>());
               if(!basket.offer(message)) {
                   throw new BusinessException(String.format("send message offer error, message: %s", msg.toString()));
               }
               if(!isSending) {
                   synchronized (mergeLock) {
                       mergeLock.notifyAll();
                   }
               }
               return messageFuture.get(clientConfig.getRPC_REQUEST_TIMEOUT(), TimeUnit.MILLISECONDS);
            }
            else {
                NettyChannel channel = clientChannelManager.acquireChannel(key);

                return sendSyncRequest(channel, msg, this::releaseChannel);
            }
        }
        catch (TimeoutException | InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new BusinessException(ex, "send sync request error :" + msg.toString());
        }
    }


    @Override
    public Object sendSyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel, Boolean> consumer) throws TimeoutException, InterruptedException {
        RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);

        try {
            MessageFuture future = super.sendAsync(channel, message, clientConfig.getRPC_REQUEST_TIMEOUT(),
                    clientConfig.getMAX_NOT_WRITEABLE_RETRY(), consumer, null);
            return future.get(clientConfig.getRPC_REQUEST_TIMEOUT(), TimeUnit.MILLISECONDS);
        }
        catch (Exception ex) {
            //fast fail
            MessageFuture messageFuture = futureMap.get(message.getId());
            if(null != messageFuture) {
                messageFuture.setResultException(ex);
            }
            throw ex;
        }

    }

    @Override
    public MessageFuture sendAsyncRequest(NettyChannel channel, Object msg, BiConsumer<NettyChannel, Boolean> consumer) {
        RpcMessage message = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);

        return super.sendAsync(channel, message, clientConfig.getRPC_REQUEST_TIMEOUT(),
                clientConfig.getMAX_NOT_WRITEABLE_RETRY(), consumer, null);
    }

    @Override
    public void sendAsyncResponse(NettyPoolKey key, RpcMessage rpcMessage, Object msg) throws Exception {

        boolean invalidate = false;
        NettyChannel channel = clientChannelManager.acquireChannel(key);
        try {
            sendAsyncResponse(channel, rpcMessage, msg);
        }
        catch (Exception ex) {
            invalidate = true;
            throw ex;
        }
        finally {
            clientChannelManager.releaseChannel(channel, invalidate);
        }
    }
    @Override
    public void sendAsyncResponse(NettyChannel channel, RpcMessage rpcMessage, Object msg)  {
        RpcMessage rpcMsg = buildResponseMessage(rpcMessage, msg, ProtocolConstants.MSGTYPE_RESPONSE);
        super.sendAsyncResponse(channel, rpcMsg, clientConfig.getRPC_REQUEST_TIMEOUT(), clientConfig.getMAX_NOT_WRITEABLE_RETRY());

    }

    @Override
    public void start() {
        clientBootstrap.start();
        if(clientConfig.isENABLE_CLIENT_BATCH_SEND_REQUEST()) {
            mergeSendExecutorService = new ThreadPoolExecutor(MAX_MERGE_SEND_THREAD, MAX_MERGE_SEND_THREAD, KEEP_ALIVE_TIME, TimeUnit.MICROSECONDS,
                    new LinkedBlockingDeque<>(),
                    new DefaultThreadFactory(getThreadPrefix(), MAX_MERGE_SEND_THREAD));
            mergeSendExecutorService.submit(new MergedSendRunnable());
        }
        apply();
    }

    @Override
    public void dispose() {
        clientBootstrap.shutdown();
        clientChannelManager.removeAll();
        super.dispose();
    }

    @Override
    public void apply() {
        if(null == servers) {
            servers = new HashSet<>();
        }
        try {
            Balancer balancer = LoadBalanceFactory.INSTANCE().loadBalancer(clientConfig.getBalanceType());
            List<SocketAddress> newSet = balancer.getAvailableInvokers(getClusterName());
            for (SocketAddress address:newSet
                 ) {
                servers.remove(address);
                NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, getClusterName(), NetUtils.toStringAddress(address), getClientId());
                NettyChannel channel = clientChannelManager.acquireChannel(key);
                if (servers.size() == 0) {
                    sendSyncRequest(channel, ping(), this::releaseChannel);
                } else {
                    sendAsyncRequest(channel, ping(), this::releaseChannel);
                }

            }
            for (SocketAddress address:servers
                 ) {
                NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, getClusterName(), NetUtils.toStringAddress(address), getClientId());
                clientChannelManager.removeChannel(key);
            }
            servers = new HashSet<>(newSet);
        }
        catch (Exception ex) {
            log.error("check connections error :" + BusinessException.exceptionFullMessage(ex));
        }
    }

    @Override
    public  NettyChannel convertTo(Channel channel) {
        NettyPoolKey poolKey = clientChannelManager.getFromAddress(NetUtils.toStringAddress(channel.remoteAddress()));
        return new NettyChannel(poolKey, channel);
    }

    @Override
    protected RpcMessage buildRequestMessage(Object msg, byte messageType) {
       return buildRequestMessage(msg, messageType, clientConfig);
    }


    @Override
    protected void closeChannel(NettyChannel channel) {

        //dont return to pool here,make borrow/return pair
        channel.getChannel().disconnect();
        channel.getChannel().close();
    }

    protected abstract String getClusterName();

    public abstract String getClientId();


    @Override
    protected IClientBootstrapPlugin[] defaultPlugins() {
        return new IClientBootstrapPlugin[] {new DefaultClientPlugin()};
    }

    protected int getMergeSendMills() {
        return MAX_MERGE_SEND_MILLS;
    }

    private String getThreadPrefix() {
        return AbstractNettyRemotingClient.MERGE_THREAD_PREFIX + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }

    protected HeartbeatMessageRequest ping() {
        HeartbeatMessageRequest request = new HeartbeatMessageRequest();
        request.setApplicationId(clientConfig.getClientAppName());
        request.setClientId(getClientId());
        return request;
    }

    private String getXid(Object msg) {
        if(BranchAbstractRequest.class.isAssignableFrom(msg.getClass())) {
            return ((BranchAbstractRequest)msg).getXid();
        }
        Object obj = ReflectUtils.getPropertyByFieldName(msg, "xid");
        if(null != obj) {
            return obj.toString();
        }
        return String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
    }

    private void releaseChannel(NettyChannel channel, Boolean invalidate) {
        try {
            clientChannelManager.releaseChannel(channel, invalidate);
        }
        catch (Exception ex) {
            throw new BusinessException(ex, ex.getMessage());
        }
    }

    class DefaultClientPlugin implements IClientBootstrapPlugin {

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
                sendAsyncRequest(nettyChannel, ping(), null);
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

    private class MergedSendRunnable implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
             while (true) {
                 isSending = true;
                 basketMap.forEach((key, queue) -> {
                     if(queue.isEmpty()) {
                         return;
                     }
                     NettyChannel nettyChannel;
                     MergeMessageRequest mergeMessage = new MergeMessageRequest();
                     mergeMessage.setApplicationId(clientConfig.getClientAppName());
                     mergeMessage.setClientId(getClientId());
                     mergeMessage.setIds(new ArrayList<>());
                     mergeMessage.setMsgs(new ArrayList<>());
                     while (!queue.isEmpty()) {
                         RpcMessage msg = queue.poll();
                         mergeMessage.getMsgs().add((AbstractMessage) msg.getBody());
                         mergeMessage.getIds().add(msg.getId());
                     }
                     try {
                         nettyChannel = clientChannelManager.acquireChannel(key);
                         sendAsyncRequest(nettyChannel, mergeMessage, AbstractNettyRemotingClient.this::releaseChannel);
                     }
                     catch (Exception ex) {
                         // fast fail
                         for (Integer msgId : mergeMessage.getIds()) {
                             MessageFuture messageFuture = futureMap.remove(msgId);
                             if (messageFuture != null) {
                                 messageFuture.setResultMessage(null);
                             }
                         }
                         log.error(String.format("batch send message error:%s, data:%s", BusinessException.exceptionFullMessage(ex),
                                 mergeMessage.toString()));
                     }
                 });
                 isSending = false;
                 try {
                     synchronized (mergeLock) {
                         mergeLock.wait(getMergeSendMills());
                     }
                 }
                 catch (InterruptedException ignored) {}
             }

        }
    }
}
