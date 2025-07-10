package com.xm.sanvanfo;

import lombok.Data;

@Data
public class CoordinatorConfig {
    private String netCardName;
    private Integer ipType;
    private String appName;
    private String leaderKey;
    private String nodeListKey;
    private Long leaderKeyTimeout;
    private String electionKey;
    private String lockWaitSuffix;
    private String lockListSuffix;
    private Integer idleWaitMills;
    private Long electionTimeout;
    private Integer zoneHour;
    private Long failWaitMills;
    private Long lockMaxWaiMills = -1L;
    private Integer retryTimes;
    private Long clientTimeoutMills;
    private Integer deleteFollowerThread;
    private String deleteFollowerPrefix;
    private Long maxProcessWaitTime;
    private Boolean detectDeadLock;
    private Long maxResponseFailMills;
    private Boolean useExistsNettyServer = false;
    private String space = "coordinator";
    private Integer dropForFailTimes = -1;
    private String readLockSuffix = "readLock";
    private String writeLockSuffix = "writeLock";
    private String notifyWaitSuffix = "notifyWait";
    private Integer repairThreadNum = 1;


    public String getAppName() {
        return "redis" + appName;
    }

}
