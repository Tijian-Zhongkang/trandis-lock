package com.xm.sanvanfo.trandiscore.proxy;

@SuppressWarnings({"unused"})
public class TrandisLockManager {

    private static ThreadLocal<Boolean> lockEnable = new ThreadLocal<>();

    static {
        lockEnable.set(true);
    }

    public static void setEnableLock(boolean lock) {
        lockEnable.set(lock);
    }

    public static boolean isEnableLock() {
        Boolean ret = lockEnable.get();
        return null == ret ? false : ret;
    }
}
