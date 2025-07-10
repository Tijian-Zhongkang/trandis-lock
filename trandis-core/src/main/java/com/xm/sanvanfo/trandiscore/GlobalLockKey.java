package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.trandiscore.transaction.GlobalKeyInterface;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
public class GlobalLockKey {

    private String xid;
    private String clientId;
    private String applicationId;
    private Object keyId;
    private Integer sectionNumber;
    private Object subKeyId;
    private Integer keyType;
    private String spaceName;
    private Boolean write;
    private Boolean lockUpgrade;
    private Integer orderBy;
    private String keyPath;

}
