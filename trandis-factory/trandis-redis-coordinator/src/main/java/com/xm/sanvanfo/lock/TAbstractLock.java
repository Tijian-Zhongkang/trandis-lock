package com.xm.sanvanfo.lock;

import com.xm.sanvanfo.CoordinatorFuture;
import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.roles.LockMessageBus;
import com.xm.sanvanfo.roles.NodeLockInfo;
import com.xm.sanvanfo.trandiscore.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
abstract class TAbstractLock {

    private static class LockBusinessException extends BusinessException {

        private static final long serialVersionUID = 5055955280529898261L;

        LockBusinessException(String message) {
            super(message);
        }
    }

    boolean acquire(AcquireRequest acquire, Long timeout, TimeUnit timeUnit) throws Exception {
        long startMills = System.currentTimeMillis();
        while(true) {
            log.debug("begin lock: {}", acquire.toString());
            CoordinatorFuture future = LockCoordinator.INSTANCE().processRequestAsync(acquire, CoordinatorConst.acquireRequestPlugin);
            Object result;
            try {
                if (timeout.equals(-1L)) {
                    result = future.get(CoordinatorConst.defaultAsyncWaitMills, TimeUnit.MILLISECONDS);
                }
                else {
                    result = future.get(timeout, timeUnit);
                }
            } catch (TimeoutException ex) {
                log.error("acquire process request wait timeout {}", acquire.toString());
                dontThrowLockBusinessRelease(acquire);
                if(timeout.equals(-1L)) {
                    throw ex;
                }
                return false;
            }

            Asserts.isTrue(result instanceof AcquireResponse);
            AcquireResponse response = (AcquireResponse) result;
            LockMessageBus.INSTANCE().acquireLock(response.getLockPath(),
                    null != response.getWaitPath() ? Collections.singletonList(response.getWaitPath()): null);
            if (response.getCode().equals(BaseResponse.ResponseCode.WAITSIGNAL.getCode())) {
                try {
                    log.debug("receive lock wait {}, response {}", acquire.toString(), response.toString());
                    boolean ret = LockMessageBus.INSTANCE().wait(acquire.getThreadId(), response.getWaitPath().getPath(), timeout, timeUnit, startMills);
                    if(!ret) {
                        dontThrowLockBusinessRelease(acquire);
                        throw new TimeoutException(String.format("acquire time out %s", acquire.toString()));
                    }
                }
                catch (Exception ex) {
                    log.error("receive lock wait timeout {}", acquire.toString());
                    dontThrowLockBusinessRelease(acquire);
                    throw ex;
                }
            } else if (!response.getCode().equals(BaseResponse.ResponseCode.SUCCESS.getCode())) {
                dontThrowLockBusinessRelease(acquire);
                throw new BusinessException(String.format("acquire fail code: %d, msg:%s", response.getCode(), response.getMsg()));
            }
            else {
                log.debug("lock success:{}", acquire.toString());
                return true;
            }
        }
    }

    void release(ReleaseRequest release) throws Exception {
        release(release, 0);
    }

    private void release(ReleaseRequest release, int count) throws Exception {
        if(count >= 5) {
            throw new LockBusinessException(String.format("release lock fail retry 5 times still timeout %s", release.toString()));
        }
        log.debug("begin release: {}", release.toString());
        CoordinatorFuture future = LockCoordinator.INSTANCE().processRequestAsync(release,  CoordinatorConst.releaseRequestPlugin);
        try {
            Object result = future.get(CoordinatorConst.defaultAsyncWaitMills, TimeUnit.MILLISECONDS);
            Asserts.isTrue(result instanceof ReleaseResponse);
            ReleaseResponse response = (ReleaseResponse)result;
            List<NodeLockInfo.LockInfo> allList = new ArrayList<>();
            if(null != response.getReleasePaths()) {
                allList.addAll(response.getReleasePaths());
            }
            if(null != response.getFailPaths()) {
                allList.addAll(response.getFailPaths());
            }
            LockMessageBus.INSTANCE().releaseLock(allList);
            if(!response.getCode().equals(BaseResponse.ResponseCode.SUCCESS.getCode())) {
                throw new LockBusinessException(String.format("release lock fail code: %d, msg:%s", response.getCode(), response.getMsg()));
            }
            log.debug("release success:{}", release.toString());
        }
        catch (TimeoutException ex) {
            log.error("release process request wait {}", release.toString());
            count++;
            release(release, count);
        }
    }

    private void dontThrowLockBusinessRelease(AcquireRequest acquire) throws Exception {
        ReleaseRequest release = LockCoordinator.INSTANCE().createFrom(acquire);
        try {
            release(release);
        }
        catch (LockBusinessException ignore) {}
    }
}
