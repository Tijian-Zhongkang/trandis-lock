package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.utils.LruThreadSafeGetter;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Supplier;

//Due to the issue of timeout, it is possible that there may be both a thread's acquire and release in the retry queue,
// at which point the original retry item needs to be overwritten
@Slf4j
class RepairRetryManager {


    static  class RetryTask implements Delayed {
        private volatile Supplier<Boolean> supplier;
        private volatile Object data;
        private final String threadId;
        private long expire;
        private final long start = Clock.systemDefaultZone().millis();



        RetryTask(Object data, Supplier<Boolean> supplier, long expire) {
            this(null, data, supplier, expire);
        }

        RetryTask(String threadId, Object data,  Supplier<Boolean> supplier, long expire) {
            this.data = data;
            this.supplier = supplier;
            this.expire = expire;
            this.threadId = threadId;
        }

        RetryTask(RetryTask task) {
            this.data = task.data;
            this.supplier = task.supplier;
            this.expire = task.expire;
            threadId = task.threadId;
        }

        void setDataAndSupplier(Object data, Supplier<Boolean> supplier) {
            this.data = data;
            this.supplier = supplier;
        }

        Boolean run() {
            log.info("run task data is {}", data.toString());
            return supplier.get();
        }

        Object getData() {return data;}

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            return start + expire - Clock.systemDefaultZone().millis();
        }

        @Override
        public int compareTo(Delayed o) {
            return (int)(o.getDelay(TimeUnit.NANOSECONDS) - getDelay(TimeUnit.NANOSECONDS));
        }
    }

    private final static Integer retryWait = 200;
    private final Queue<RepairRetryManager.RetryTask> retryQueue = new DelayQueue<>();
    private final ConcurrentHashMap<String, RepairRetryManager.RetryTask> threadRetryMap = new ConcurrentHashMap<>();
    private final LruThreadSafeGetter<String, Object> lruRetryLock = new LruThreadSafeGetter<>(3600L * 24, 30L,Object::new);
    private final ExecutorService fixRetryExecutor;
    private volatile boolean shutdown = false;
    private final String id;

    RepairRetryManager(String id, CoordinatorConfig config) {
        this.id = id;
        RepairRetryManager manager = this;
        fixRetryExecutor = Executors.newSingleThreadExecutor();
        fixRetryExecutor.execute(()->{
            while (!shutdown && !Thread.currentThread().isInterrupted()) {
                RepairRetryManager.RetryTask task = retryQueue.poll();
                synchronized (manager) {
                    if (null == task) {
                        try {
                            manager.wait(retryQueue.isEmpty() ? config.getIdleWaitMills() : retryWait);
                        } catch (InterruptedException ignore) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                while (task != null) {
                    Boolean result = task.run();
                    if(!result) {
                        retryQueue.offer(new RepairRetryManager.RetryTask(task));
                    }
                    else {
                        if(task.threadId != null) {
                            Object data = task.data;
                            Supplier<Boolean> supplier = task.supplier;
                            synchronized (lruRetryLock.get(task.threadId)) {
                                if(data.equals(task.getData()) && supplier.equals(task.supplier)) {
                                    threadRetryMap.remove(task.threadId);
                                }
                                else {
                                    retryQueue.offer(new RepairRetryManager.RetryTask(task));
                                }
                            }
                        }
                    }
                    task = retryQueue.poll();
                }
            }
        });
    }

    void addRetryTask(Object data, Supplier<Boolean> task) {
        retryQueue.offer(new RetryTask(data, task, retryWait));
        synchronized (this) {
            this.notifyAll();
        }
    }

    void addThreadRetryTask(String threadId, Object data, Supplier<Boolean> task) {
        //Lock must be used to ensure consistency between map and queue
        synchronized (lruRetryLock.get(threadId)) {
            RetryTask retryTask = new RetryTask(threadId, data, task, retryWait);
            RetryTask preTask = threadRetryMap.putIfAbsent(threadId, retryTask);
            if (null != preTask) {
                preTask.setDataAndSupplier(data, task);
            } else {
                retryQueue.offer(retryTask);
            }
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    boolean hasThreadRetryTask(String threadId) {
        synchronized (lruRetryLock.get(threadId)) {
            return threadRetryMap.containsKey(threadId);
        }
    }

    void shutdown() {
        this.shutdown = true;
        lruRetryLock.dispose();
        fixRetryExecutor.shutdown();
        if (!fixRetryExecutor.isTerminated()) {
            try {
                boolean ret = fixRetryExecutor.awaitTermination(60, TimeUnit.SECONDS);
                if(!ret) {
                    log.error("repair fixRetry fail shutdown");
                }
            }
            catch (InterruptedException ignore) {}
        }

    }

    List<CoordinatorMessage> getSelfRepairCoordinatorMessages() {
        List<CoordinatorMessage> messages = new ArrayList<>();
        for (RetryTask task:retryQueue
        ) {
            Object data = task.getData();
            if(data instanceof CoordinatorMessage) {
                CoordinatorMessage message = (CoordinatorMessage)data;
                Object body = message.getMessage().getBody();
                if(body instanceof PluginRequest) {
                    PluginRequest request = (PluginRequest)body;
                    Object pluginObj = request.getBodyObj();
                    if(CoordinatorMessageWare.class.isAssignableFrom(pluginObj.getClass())) {
                        CoordinatorMessageWare messageWare = (CoordinatorMessageWare)pluginObj;
                        if(messageWare.getCoordinatorId().equals(id)) {
                            messages.add(message);
                        }
                    }
                }

            }
        }
        return messages;
    }
}
