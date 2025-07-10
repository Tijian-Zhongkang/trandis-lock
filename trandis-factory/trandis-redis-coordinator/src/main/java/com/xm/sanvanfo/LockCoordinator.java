package com.xm.sanvanfo;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.CommonUtils;
import com.xm.sanvanfo.common.utils.EnvUtils;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.processor.*;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.roles.*;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.time.Clock;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused"})
@Slf4j
public class LockCoordinator {

    private static final LockCoordinator instance = new LockCoordinator();
    private IRole.RoleType roleType = IRole.RoleType.Candidate;
    private ExecutorService executorService;
    private String coordinatorId;
    private IRedisLocker locker;
    private IRole currentRole = null;
    private final Object waitObject = new Object();
    private NodeInfo nodeInfo = null;
    private CoordinatorConfig config;
    private NettyClientConfig nettyClientConfig;
    private EventExecutorGroup eventExecutorGroup;
    private static final int MASK = 0x7FFFFFFF;
    private final AtomicInteger atom = new AtomicInteger(0);
    private final Map<String, CoordinatorFuture> mapFutures = new ConcurrentHashMap<>();
    private ICoordinatorPlugin plugin;
    private NettyServerConfig serverConfig;
    private IServerBootstrapPlugin[] serverPlugins;
    private ChannelHandler[] serverChannelHandlers;
    private Compressor compressor;
    private Serializer serializer;
    private Deque<CoordinatorMessage> deque = new ConcurrentLinkedDeque<>();

    public static LockCoordinator INSTANCE() {
        return instance;
    }

    private LockCoordinator() {}

    public void configuration(CoordinatorConfig config) {
        this.config = config;
    }

    public void setLocker(IRedisLocker locker) {
        this.locker = locker;
    }

    public void setNettyClientConfig(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup) {
        this.nettyClientConfig = nettyClientConfig;
        this.eventExecutorGroup = eventExecutorGroup;
    }

    public void setNettyServerConfig(NettyServerConfig serverConfig, IServerBootstrapPlugin[] plugins, ChannelHandler... channelHandlers) {
        this.serverConfig = serverConfig;
        this.serverPlugins = plugins;
        this.serverChannelHandlers = channelHandlers;
    }

    public void setCompressor(Compressor compressor) {
         this.compressor = compressor;
    }

    public void setSerializer(Serializer serializer) {
       this.serializer = serializer;
    }

    public void registerPlugin(ICoordinatorPlugin plugin) {
        this.plugin = plugin;
    }

    //this function will be no-waiting, you can add message, but message can only be processed after the initialization of the current role is completed
    public void start() {
        startInner(false);
    }

    //this function will be waiting until current role can process message
    public void startWaitSuccess() {
        startInner(true);
    }

    public void close() {
        synchronized (waitObject) {
            waitObject.notifyAll();
        }
        if(null != currentRole) {
            currentRole.close();
        }
        executorService.shutdown();
        deque.clear();

    }

    public IRole getCurrentRole() {
        return currentRole;
    }

    public void process(Channel channel, RpcMessage rpcMessage) {
        Object body = rpcMessage.getBody();
        Asserts.isTrue(body instanceof PluginRequest);
        PluginRequest request = (PluginRequest)body;
        Object obj = request.getBodyObj();
        if(CoordinatorMessageWare.class.isAssignableFrom(obj.getClass())) {
            CoordinatorMessageWare messageWare = (CoordinatorMessageWare)obj;
            String dest = messageWare.getDestCoordinatorId();
            if(StringUtils.isNotEmpty(dest) && !dest.equals(coordinatorId)) {
                log.warn("skip old coordinator message:{}", messageWare.toString());
                return;
            }
        }
        pushMessage(new CoordinatorMessage(rpcMessage));
    }

    public CoordinatorFuture processRequestAsync(CoordinatorMessageWare message,  String plugin) {
        CoordinatorFuture future = new CoordinatorFuture();
        if(null == message.getId()) {
            message.setId(coordinatorId + "-" + getNextMessageId());
        }
        message.setCoordinatorId(coordinatorId);
        message.setAppName(config.getAppName());
        PluginRequest request = new PluginRequest();
        request.setPlugin(plugin);
        request.setObj(message);
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(request);
        rpcMessage.setBodyType(MessageTypeAware.MessageType.TYPE_PLUGIN_REQUEST.getCode());
        CoordinatorMessage coordinatorMessage = new CoordinatorMessage(rpcMessage);
        future.setMessage(coordinatorMessage);
        pushMessage(coordinatorMessage);
        mapFutures.put(message.getId(), future);
        return future;
    }

    public CoordinatorFuture getFuture(String id) {
        return mapFutures.get(id);
    }

    public void processResponse(PluginResponse message) {
        Object body = message.getObj();
        Asserts.isTrue(CoordinatorMessageWare.class.isAssignableFrom(body.getClass()));
        CoordinatorMessageWare messageWare = (CoordinatorMessageWare)body;
        if(!coordinatorId.equals(messageWare.getCoordinatorId()) && !coordinatorId.equals(messageWare.getDestCoordinatorId())) {
            log.warn("skip old coordinator message:{}", message.toString());
            return;
        }
        String id = messageWare.getId();
        CoordinatorFuture future = mapFutures.remove(id);
        if(null != future) {
            if(future.isTimeout()) {
                //need release lock in these case: (1) old leader crashed, new leader rebuild lock (2)  timeout threshold future time out but acquire success
                //lock exception release maybe not truly
                log.warn("future is timeout {}", messageWare.toString());
                if(messageWare instanceof AcquireResponse) {
                    AcquireResponse response = (AcquireResponse)messageWare;
                    if(!response.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode())) {
                        ReleaseRequest releaseRequest = createFrom(response.getAcquireRequest());
                        processRequestAsync(releaseRequest, CoordinatorConst.releaseRequestPlugin);
                    }
                }
            }
            else {
                future.setResultMessage(messageWare);
            }
        }
    }

    public void dropFuture(CoordinatorMessageWare messageWare) {
        String id = messageWare.getId();
        CoordinatorFuture future = mapFutures.remove(id);
        if(null != future) {
            log.warn("drop message {}", messageWare.toString());
        }
    }

    public ReleaseRequest createFrom(AcquireRequest body) {
        ReleaseRequest releaseRequest = new ReleaseRequest();
        releaseRequest.setThreadId(body.getThreadId());
        releaseRequest.setCoordinatorId(body.getCoordinatorId());
        if(null != body.getWritePath()) {
            releaseRequest.setWritePath(body.getWritePath().getPath());
            releaseRequest.setWriteEntrantTimes(body.getWritePath().getEntrantTimes() > 0 ?
                    body.getWritePath().getEntrantTimes() - 1 : body.getWritePath().getEntrantTimes());
        }
        if(null != body.getReadPath()) {
            releaseRequest.setReadPath(body.getReadPath().getPath());
            releaseRequest.setReadEntrantTimes(body.getReadPath().getEntrantTimes() > 0 ?
                    body.getReadPath().getEntrantTimes() - 1 : body.getReadPath().getEntrantTimes());
        }
        return releaseRequest;
    }

    public void setServerInfo(AbstractNettyRemoting server, String serverId) {
        if(null == nodeInfo) {
            nodeInfo = new NodeInfo();
        }
        this.nodeInfo.setRemoting(server);
        this.nodeInfo.setServerId(serverId);
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }


    public void pushMessage(CoordinatorMessage message) {
        deque.offerLast(message);
        synchronized (waitObject) {
            waitObject.notifyAll();
        }
    }

    public void pushMessageFirst(CoordinatorMessage message) {
        deque.offerFirst(message);
        synchronized (waitObject) {
            waitObject.notifyAll();
        }
    }

    public void logLockInfo() {
        locker.logLockInfo();
    }

    void setBootstrapServerInfo(String serverAppName, String ip, int listenPort) {
        if(null == nodeInfo) {
            nodeInfo = new NodeInfo();
        }
        this.nodeInfo.setBootstrapServerInfo(new BootstrapServerInfo(ip, listenPort, serverAppName,
                Clock.systemDefaultZone().millis(), coordinatorId, BootstrapServerInfo.Status.ACTIVE));
    }

    private void startInner(boolean waitSuccess) {
        InetAddress address = EnvUtils.getIpAddress(config.getNetCardName(), config.getIpType());
        coordinatorId = String.format("%s:%d-%s", null == address ? "no known" : address.getHostAddress(),
                serverConfig.getListenPort(), CommonUtils.uuid());
        LockMessageBus.INSTANCE().setLockCoordinatorInfo(config);
        executorService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("LockCoordinator-Queue"));
        PluginLoader.INSTANCE().registerPlugin(new HeartBeatRequestPluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new HeartBeatResponsePluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new AcquireRequestPluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new AcquireResponsePluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new ReleaseRequestPluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new ReleaseResponsePluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new GetLockBusRequestPluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new GetLockBusResponsePluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new NotifyRequestPluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new NotifyResponsePluginProcessor());
        PluginLoader.INSTANCE().registerPlugin(new CloseRequestPluginProcessor());
        executorService.submit(new LockCoordinatorRunnable(waitSuccess ? this : null));
        if(waitSuccess) {
            synchronized (this) {
                try {
                    this.wait();
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private int getNextMessageId() {
        return atom.incrementAndGet() & MASK;
    }

    private IRole getRole(IRole.RoleType roleType) {

           if(null != currentRole && currentRole.getRole().equals(roleType)) {
                 return currentRole;
           }
           if(null != currentRole) {
               currentRole.shutdown();
               if(null != plugin) {
                   plugin.roleClose(currentRole.getRole());
               }
               currentRole = null;
           }
            switch (roleType) {
                case Candidate:
                    currentRole = new Candidate(config, coordinatorId, locker);
                    break;
                case Follower:
                    currentRole = new Follower(config, nettyClientConfig, coordinatorId, locker, eventExecutorGroup, compressor, serializer);
                    break;
                case Leader:
                    currentRole = new Leader(config, coordinatorId, locker, serverConfig, compressor, serializer, serverPlugins, serverChannelHandlers);
                    break;
                default:
                    currentRole = null;
            }
           if(null != plugin) {
               plugin.roleInit(currentRole.getRole());
           }
           return currentRole;
    }


    private class LockCoordinatorRunnable implements Runnable {

        private final Object waitObj;

        private LockCoordinatorRunnable(Object waitObj) {
            this.waitObj = waitObj;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    IRole role = getRole(roleType);
                    boolean ret = role.init();
                    IRole.RoleType rType = role.checkChange();
                    if (roleType.equals(rType)) {
                        boolean isReady = !roleType.equals(IRole.RoleType.Candidate) && ret;
                        if(null != waitObj) {
                            synchronized (waitObj) {
                                waitObj.notifyAll();
                            }
                        }
                        synchronized (waitObject) {
                            if(deque.isEmpty() || !ret) {
                                try {
                                    waitObject.wait(config.getIdleWaitMills());
                                } catch (InterruptedException ignore) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }

                        if(isReady) {
                            currentRole.process(deque);
                        }

                    } else {
                        roleType = rType;
                    }
                }
                catch (Exception ex) {
                    InetAddress address = EnvUtils.getIpAddress(config.getNetCardName(), config.getIpType());
                    log.error("coordinator error id:{} address:{} message:{}", coordinatorId,
                            null == address ? "no known" : address.getHostAddress(),
                            BusinessException.exceptionFullMessage(ex));
                }
            }
        }
    }
}
