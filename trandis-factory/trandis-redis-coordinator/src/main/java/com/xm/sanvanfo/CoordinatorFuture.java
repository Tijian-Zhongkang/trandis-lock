package com.xm.sanvanfo;

import com.xm.sanvanfo.trandiscore.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused")
public class CoordinatorFuture {
    @Getter
    @Setter
    private CoordinatorMessage message;
    @Getter
    @Setter
    private long timeout;
    private volatile boolean bTimeout = false;
    private long start = System.currentTimeMillis();
    private transient CompletableFuture<Object> origin = new CompletableFuture<>();

    public boolean isTimeout() {
        return bTimeout || (timeout > 0 && System.currentTimeMillis() - start > timeout);
    }

    public Object get(long timeout, TimeUnit unit) throws TimeoutException,
            InterruptedException {
        Object result;
        try {
            this.timeout = unit.toMillis(timeout);
            result = origin.get(timeout, unit);
        } catch (ExecutionException e) {
            throw new BusinessException(e, e.getMessage());
        } catch (TimeoutException e) {
            bTimeout = true;
            throw new TimeoutException("coordinator future cost " + (System.currentTimeMillis() - start) + " ms");
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

    public void setResultException(Throwable throwable) {
        origin.completeExceptionally(throwable);
    }
}
