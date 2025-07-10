package com.xm.sanvanfo.trandiscore.globallock;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface GlobalLockProvider {

    void acquire(List<String> readPaths, List<String> writePaths) throws Exception;

    Boolean acquire(List<String> readPaths, Long readTimeout,  TimeUnit readTimeUnit,
                          List<String> writePaths,  Long writeTimeout, TimeUnit writeUnit) throws Exception;

    void release(List<String> readPaths, List<String> writePaths) throws Exception;

    List<String> releaseRead(List<String> read, List<String> write) throws Exception;
}
