package com.xm.sanvanfo.trandiscore.transaction.redis;

import lombok.Getter;
import lombok.Setter;


@Getter
public class RedisBranchTransactionConfiguration {

    private String snapshotSuffix;
    private String transactionSuffix;
    private String backupSuffix;
    private String sessionSuffix;
    private Integer defaultTimeout;
    private String defaultGroupName;
    private String networkCardName;
    private Integer ipType;
    private String applicationId;
    private Boolean hashWholeLock;
    private Boolean noSupportException;
    private int port;
    @Setter
    private String spaceName;


    public RedisBranchTransactionConfiguration(String snapshotSuffix, String transactionSuffix, String backupSuffix,
                                               String sessionSuffix, Integer defaultTimeout, String defaultGroupName,
                                               String networkCardName, Integer ipType, String applicationId, Boolean hashWholeLock,
                                               Boolean noSupportException, int port) {
        this.snapshotSuffix = snapshotSuffix;
        this.transactionSuffix = transactionSuffix;
        this.backupSuffix = backupSuffix;
        this.sessionSuffix = sessionSuffix;
        this.defaultTimeout = defaultTimeout;
        this.defaultGroupName = defaultGroupName;
        this.networkCardName = networkCardName;
        this.ipType = ipType;
        this.applicationId = applicationId;
        this.hashWholeLock = hashWholeLock;
        this.noSupportException = noSupportException;
        this.port = port;
    }
}
