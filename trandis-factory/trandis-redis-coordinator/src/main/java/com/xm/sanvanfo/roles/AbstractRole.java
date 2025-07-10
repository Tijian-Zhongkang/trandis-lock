package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.*;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.processor.ICoordinatorProcessPlugin;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.MessageFuture;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.IPluginMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Slf4j
abstract class AbstractRole implements IRole {

     final String id;
     final IRedisLocker locker;
     final CoordinatorConfig coordinatorConfig;
     final LockCoordinator lockCoordinator;
     private final IRole.RoleType roleType;
    private final AliveRunnable aliveRunnable;
    private volatile boolean aliveInit = true;
    private ExecutorService checkAliveExecutor;
    private final Map<Class<? extends CoordinatorMessageWare>, IRequestProcessor> requestProcessorMap = new ConcurrentHashMap<>();
    private static final String processorClassNameFormat = "com.xm.sanvanfo.roles.%s%sProcessor";

    AbstractRole(CoordinatorConfig config, String id, IRedisLocker locker, IRole.RoleType roleType) {
        this.lockCoordinator = LockCoordinator.INSTANCE();
        this.coordinatorConfig = config;
        this.id = id;
        this.locker = locker;
        this.roleType = roleType;
        aliveRunnable = new AliveRunnable();
    }

    @Override
    public RoleType getRole() {
        return roleType;
    }

    @Override
    public void process(Deque<CoordinatorMessage> deque) {

        CoordinatorMessage message = deque.pollFirst();
        while (null != message && !Thread.currentThread().isInterrupted()) {
            if(roleIsChanged()) {
                deque.offerFirst(message);
                break;
            }
            try {
                Asserts.isTrue(IPluginMessage.class.isAssignableFrom(message.getMessage().getBody().getClass()));
                if (message.getAddTime().equals(0L)) {
                    message.setAddTime(Clock.systemDefaultZone().millis());
                }
                String plugin = ((IPluginMessage) message.getMessage().getBody()).getPluginName();
                IProcessorPlugin processorPlugin = PluginLoader.INSTANCE().load(IProcessorPlugin.class, plugin);
                Asserts.isTrue(ICoordinatorProcessPlugin.class.isAssignableFrom(processorPlugin.getClass()));
                ICoordinatorProcessPlugin coordinatorProcessorPlugin = (ICoordinatorProcessPlugin) processorPlugin;
                Class clazz = coordinatorProcessorPlugin.getSerializerType();
                ProcessStatus ret = processPluginBody(message, clazz);
                if(ret.equals(ProcessStatus.REPROCESSTAIL)) {
                    deque.offerLast(message);
                }
                else if(ret.equals(ProcessStatus.REPROCESSHEAD)) {
                    deque.offerFirst(message);
                }

            } catch (Exception ex) {
               if(!checkCoordinatorMessageDrop(message, ex, "process")) {
                    boolean ret = false;
                    try {
                        ret = init();
                    }
                    catch (Exception e) {
                        log.error("init error" + BusinessException.exceptionFullMessage(e));
                    }
                    message.setFailTimes(message.getFailTimes() + 1);
                    deque.offerFirst(message);
                    if(!ret) {
                        long time = (long) Math.pow(2, message.getFailTimes());
                        try {
                            Thread.sleep(time * 1000);
                        } catch (InterruptedException ignore) {
                        }
                        break;
                    }
                }
               else {
                   dropFuture(message);
               }
            }
            message = deque.pollFirst();
        }
    }

    @Override
    public void shutdown() {
        aliveInit = false;
        synchronized (aliveRunnable) {
            aliveRunnable.notifyAll();
        }
        checkAliveExecutor.shutdown();
    }

    @Override
    public void directProcess(CoordinatorMessage message, CoordinatorMessageWare body) {log.info("default  get lock function role is {}", roleType);}

    void startAliveCheck() {
        //sync check once in order to get runtime parameters
        try {
            aliveCheck();
        }
        catch (Exception ex) {
            throw new BusinessException(ex, id + " start alive check error");
        }
        checkAliveExecutor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("Role-CheckAlive"));
        checkAliveExecutor.execute(aliveRunnable);
    }

    abstract void aliveCheck() throws Exception;

    abstract boolean roleIsChanged();

    boolean checkCoordinatorMessageDrop(CoordinatorMessage message, Throwable ex, String action) {
        log.info(String.format("%s message check drop msg : %s, error:%s", action, message, BusinessException.exceptionFullMessage(ex)));
        long now = Clock.systemDefaultZone().millis();
        if (now - message.getAddTime() > coordinatorConfig.getMaxProcessWaitTime()
                || (!coordinatorConfig.getDropForFailTimes().equals(-1) && message.getFailTimes() >= coordinatorConfig.getDropForFailTimes())) {
            log.warn("drop message, message is {} now:{}", message.toString(), now);
            return true;
        }
        return false;
    }

    void dropFuture(CoordinatorMessage message) {
        Object body = message.getMessage().getBody();
        Asserts.isTrue(body instanceof PluginRequest);
        PluginRequest request = (PluginRequest)body;
        Object pluginBody = request.getBodyObj();
        if(null != pluginBody && CoordinatorMessageWare.class.isAssignableFrom(pluginBody.getClass())) {
            lockCoordinator.dropFuture((CoordinatorMessageWare)pluginBody);
        }

    }

    boolean checkSkip(Object bodyObj) {
        return false;
    }

    PluginResponse buildPluginResponse(Object obj, String pluginName) {
        PluginResponse response = new PluginResponse();
        response.setObj(obj);
        Asserts.isTrue(BaseResponse.class.isAssignableFrom(obj.getClass()));
        BaseResponse baseResponse = (BaseResponse)obj;
        response.setCode(baseResponse.getCode());
        response.setMsg(baseResponse.getMsg());
        response.setPlugin(pluginName);
        return response;
    }

    void sendRequestTimeout(MessageFuture future, BiConsumer<Object[], PluginRequest> callBack) {
        Object body = future.getRpcMessage().getBody();
        if(!(body instanceof PluginRequest)) {
            return;
        }
        PluginRequest request = (PluginRequest)body;
        Object obj = request.getBodyObj();
        if(!CoordinatorMessageWare.class.isAssignableFrom(obj.getClass())) {
            return;
        }
        callBack.accept(new Object[]{obj, body}, request);
    }

    CoordinatorConfig getConfig() {
        return coordinatorConfig;
    }

    String getId(){
        return id;
    }

    @SuppressWarnings("unchecked")
    IRequestProcessor get(Class<? extends CoordinatorMessageWare> clazz) {
        return requestProcessorMap.computeIfAbsent(clazz, o->{
            Class processorClazz;
            String className = String.format(processorClassNameFormat, roleType.toString(), clazz.getSimpleName());
            try {
                processorClazz = Class.forName(className);
            }
            catch (ClassNotFoundException ex) {
                log.info("get request processor {} class not found", String.format(processorClassNameFormat, roleType.toString(), clazz.getSimpleName()));
                processorClazz = DefaultRequestProcessor.class;
            }
            try {
                Constructor constructor = processorClazz.getDeclaredConstructor(IRole.class);
                Object obj = constructor.newInstance(this);
                return (IRequestProcessor)obj;
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "get request processor error");
            }

        });
    }

    @SuppressWarnings("unchecked")
    private ProcessStatus processPluginBody(CoordinatorMessage message, Class clazz) throws Exception {
        Object body = message.getMessage().getBody();
        Asserts.isTrue(body instanceof PluginRequest);
        PluginRequest request = (PluginRequest)body;
        if(checkSkip(request.getBodyObj())) {
            return ProcessStatus.SKIP;
        }
        Asserts.isTrue(CoordinatorMessageWare.class.isAssignableFrom(clazz));
        IRequestProcessor processor = get(clazz);
        if(IRequestAsyncProcessor.class.isAssignableFrom(processor.getClass())) {
            return ((IRequestAsyncProcessor)processor).processAsync(message, (CoordinatorMessageWare) request.getBodyObj());
        }
        return processor.process(message, (CoordinatorMessageWare) request.getBodyObj());
    }


    enum ProcessStatus {
        OK,
        SKIP,
        REPROCESSHEAD,
        REPROCESSTAIL,
        REPAIRING
    }

    private  class AliveRunnable implements Runnable {

        @Override
        public void run() {
            while (aliveInit && !Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    try {
                        this.wait(coordinatorConfig.getIdleWaitMills());
                    }
                    catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                }
                try {
                    if(aliveInit) {
                        aliveCheck();
                    }
                }
                catch (Exception ex) {
                    log.error("alive check exception id is {}, exception is {}", id, BusinessException.exceptionFullMessage(ex));
                }
            }
        }
    }

}
