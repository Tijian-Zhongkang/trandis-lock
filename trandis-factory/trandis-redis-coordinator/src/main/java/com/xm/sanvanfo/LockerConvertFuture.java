package com.xm.sanvanfo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

//note:
//Consumers must be in other threads, as in complete, it may block the thread causing RedisFuture. get to block
// (e.g: in lettuce-nio-Event Consumers include business locks, and other threads also include business locks, while other threads also include RedisFuture. get)
@SuppressWarnings("unused")
public class LockerConvertFuture<V> extends CompletableFuture<V> implements IContainNoScript {

    @AllArgsConstructor
    @Data
    public static class Scripture {
        private Supplier<CompletableFuture<Object>> func;
    }

    private final ExecutorService service;
    private final Scripture scripture;
    private final List<BiConsumer<Object, Throwable>> consumerList = Collections.synchronizedList(new ArrayList<>());

    public LockerConvertFuture(ExecutorService service, Scripture scripture){
        this.scripture = scripture;
        this.service = service;
    }

    public LockerConvertFuture(ExecutorService service,Scripture scripture, CompletionStage<?> completionStage, Function<LockerConvertFuture<V>, V> converter) {
        this.scripture = scripture;
        this.service = service;
        completionStage.thenAccept(v -> complete(converter.apply(this)))
                .exceptionally(throwable -> {
                    completeExceptionally(throwable);
                    return null;
                });
    }

    public LockerConvertFuture(ExecutorService service, Scripture scripture, Map<?, ? extends CompletableFuture<?>> executions, Function<LockerConvertFuture<V>, V> converter) {

        this.scripture = scripture;
        this.service = service;
        CompletableFuture.allOf(executions.values().toArray(new CompletableFuture<?>[0]))
                .thenRun(() -> complete(converter.apply(this))).exceptionally(throwable -> {
            completeExceptionally(throwable);
            return null;
        });
    }

    public void addConsumerListener(BiConsumer<Object, Throwable> consumer) {
        consumerList.add(consumer);
        if(this.isDone()) {
            service.execute(()-> {
                try {
                    V result = get();
                    consumer.accept(result, null);
                } catch (Exception ex) {
                    consumer.accept(null, ex);
                }
            });
        }

    }

    @Override
    public boolean complete(V v) {
        boolean ret = super.complete(v);
        for (BiConsumer<Object, Throwable> consumer:consumerList
             ) {
            service.execute(()-> {
                consumer.accept(v, null);
            });
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean completeExceptionally(Throwable throwable) {
        if(!exceptionNotContainsNoScriptError(throwable) && null != scripture) {
            scripture.func.get().thenAccept(v -> complete((V)v));
            return false;
        }
        else {
            boolean ret = super.completeExceptionally(throwable);
            for (BiConsumer<Object, Throwable> consumer:consumerList
            ) {
                service.execute(()-> {
                    consumer.accept(null, throwable);
                });
            }
            return ret;
        }
    }


}
