package com.xm.sanvanfo.trandiscore.netty;

import com.xm.sanvanfo.trandiscore.BusinessException;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class MessageFuture {

    @Getter
    @Setter
    private RpcMessage rpcMessage;
    @Getter
    private long timeout;
    private long start = System.currentTimeMillis();
    private transient CompletableFuture<Object> origin = new CompletableFuture<>();

    public boolean isTimeout() {
        return System.currentTimeMillis() - start > timeout;
    }

    void setTimeout(HashedWheelTimer timer, long timeout, Consumer<MessageFuture> futureConsumer) {
        this.timeout = timeout;
        timer.newTimeout(new MessageTimerTask(this, futureConsumer), this.timeout, TimeUnit.MILLISECONDS);
    }

    public Object get(long timeout, TimeUnit unit) throws TimeoutException,
            InterruptedException {
        Object result;
        try {
            result = origin.get(timeout, unit);
        } catch (ExecutionException e) {
            throw new BusinessException(e, e.getMessage());
        } catch (TimeoutException e) {
            throw new TimeoutException("message future cost " + (System.currentTimeMillis() - start) + " ms");
        }

        if (result instanceof RuntimeException) {
            throw (RuntimeException)result;
        } else if (result instanceof Throwable) {
            throw new RuntimeException((Throwable)result);
        }

        return result;
    }

    public Object get() throws InterruptedException, ExecutionException {
        return origin.get();
    }

    @SuppressWarnings({"WeakerAccess"})
    public void setResultMessage(Object obj) {
        origin.complete(obj);
    }

    @SuppressWarnings({"WeakerAccess"})
    public void setResultException(Throwable throwable) {
        origin.completeExceptionally(throwable);
    }

    private static class MessageTimerTask implements TimerTask {

        private final Consumer<MessageFuture> consumer;
        private final MessageFuture messageFuture;

        private MessageTimerTask(MessageFuture messageFuture, Consumer<MessageFuture> consumer) {
            this.messageFuture = messageFuture;
            this.consumer = consumer;
        }

        @Override
        public void run(Timeout timeout)  {
            consumer.accept(messageFuture);
        }
    }
}
