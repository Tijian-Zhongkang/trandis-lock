package com.xm.sanvanfo.common.utils;


import com.xm.sanvanfo.common.Disposable;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
@SuppressWarnings("unused")
public class LruThreadSafeGetter<K,V> implements Disposable {
    private final Supplier<V> supplier;
    private final Function<K, V> function;
    private final ConcurrentHashMap<K,Value> map = new ConcurrentHashMap<>();
    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final Object lock = new Object();
    private final Long timeout;
    private final Long checkPeriod;
    private boolean useFinish = false;

    public LruThreadSafeGetter(Long timeout, Long checkPeriod, Supplier<V> supplier) {
        this.supplier = supplier;
        this.timeout = timeout;
        this.checkPeriod = checkPeriod;
        this.function = null;
    }

    public LruThreadSafeGetter(Long timeout, Long checkPeriod, Function<K, V> function) {
        this.supplier = null;
        this.timeout = timeout;
        this.checkPeriod = checkPeriod;
        this.function = function;
    }

    public LruThreadSafeGetter(Long timeout, Long checkPeriod, Function<K, V> function, boolean useFinish) {
        this(timeout, checkPeriod, function);
        this.useFinish = useFinish;
    }

    public LruThreadSafeGetter(Long timeout, Long checkPeriod, Supplier<V> supplier, boolean useFinish) {
        this(timeout, checkPeriod, supplier);
        this.useFinish = useFinish;
    }

    public V get(K k) {
        synchronized (lock) {
            V v = supplier == null && function != null ? function.apply(k) : supplier == null ? null : supplier.get();
            if(null == v) {
                throw new RuntimeException("either function or supplier can not be null");
            }
            Value value = map.computeIfAbsent(k, r-> new Value(v, null, false));
            if(null == value.getRefreshTime()){
                    timer.newTimeout(new LruTimerTask(k, this::checkRemove), checkPeriod, TimeUnit.SECONDS);
            }
            if(useFinish && value.isActive()) {
                throw new RuntimeException("useFinish is true but this key is using by other, if multi thread you can lock this object");
            }
            value.setActive(true);
            value.setRefreshTime(new Date());
            return value.getV();
        }
    }

    public void finish(K k) {
        synchronized (lock) {
            if(map.containsKey(k)) {
                Value value = map.get(k);
                value.setActive(false);
                value.setRefreshTime(new Date());
            }
        }
    }


    private void checkRemove(K k) {
        synchronized (lock) {
            if(map.containsKey(k)) {
                Value value = map.get(k);
                Date now = new Date();
                if(now.getTime() - value.getRefreshTime().getTime() >= timeout * 1000 && (!useFinish || !value.isActive())) {
                    map.remove(k);
                }
                else {
                    timer.newTimeout(new LruTimerTask(k, this::checkRemove), checkPeriod, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Override
    public void dispose() {
        timer.stop();
    }

    private class LruTimerTask implements TimerTask {

        private final K key;
        private final Consumer<K> consumer;
        LruTimerTask(K key, Consumer<K> consumer) {
            this.key = key;
            this.consumer = consumer;
        }

        @Override
        public void run(Timeout timeout) {
            consumer.accept(key);
        }
    }

    @AllArgsConstructor
    @Data
    private class Value {
        private V v;
        private Date refreshTime;
        private boolean active;
    }
}
