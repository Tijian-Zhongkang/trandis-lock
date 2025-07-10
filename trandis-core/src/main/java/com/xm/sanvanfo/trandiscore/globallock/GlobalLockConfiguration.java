package com.xm.sanvanfo.trandiscore.globallock;

import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;

@Getter
public class GlobalLockConfiguration {

    private String space;
    private Object framework;
    private Long defaultTimeout;

    private GlobalLockConfiguration() {}

    public static GlobalLockConfiguration create(String space, Object framework, Long defaultTimeout) {
        GlobalLockConfiguration configuration = new GlobalLockConfiguration();
        configuration.space = space;
        configuration.framework = framework;
        configuration.defaultTimeout = defaultTimeout;
        return configuration;
    }

}
