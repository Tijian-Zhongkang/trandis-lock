package com.xm.sanvanfo.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class RetryUtils {


    public enum RetryType {
        FIXED,
        EXP
    }

    public static <T> T invokeRetryTimes(String name,  Supplier<T> function, Predicate<T> predicate, int times, RetryType type, long millis) {
        int retry = -1;
        T error = null;
        RuntimeException throwEx = null;
        while (retry < times) {
            try {
                T t = function.get();
                if (null == predicate || predicate.test(t)) {
                    return t;
                }
                error = t;
            }
            catch (RuntimeException ex) {
                throwEx = ex;
                log.info(String.format("%s retry times:%d", name, retry + 1));
             }
            retry++;
            if(retry >= times) {
                break;
            }
            try {
                Thread.sleep(getMillis(type, millis, retry));
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if(null != throwEx) {
            throw throwEx;
        }
        return error;
    }

    public static <T> void invokeRetryTimes(String name, Consumer<T> function, int times, RetryType type, long millis) {
        int retry = -1;
        RuntimeException throwEx = null;
        while (retry < times) {
            try {
                function.accept(null);
                return;
            }
            catch (RuntimeException ex) {
                throwEx = ex;
                log.info(String.format("%s retry times:%d", name, retry + 1));
            }
            retry++;
            if(retry >= times) {
                break;
            }
            try {
                Thread.sleep(getMillis(type, millis, retry));
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }
        if(null != throwEx) {
            throw throwEx;
        }
    }

    private static long getMillis(RetryType type, long millis, int retry) {
         if(type.equals(RetryType.FIXED)) {
             return millis;
         }
         if(type.equals(RetryType.EXP)) {
             if(retry <= 0) {
                 return millis;
             }
             SecureRandom r = new SecureRandom();
             int exp = r.nextInt() % retry;
             return (long)Math.pow(2, exp) * millis;
         }
         throw new RuntimeException("do not support type");
    }


}
