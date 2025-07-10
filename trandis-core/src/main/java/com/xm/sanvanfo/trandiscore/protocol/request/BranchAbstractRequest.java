package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

@Data
public abstract class BranchAbstractRequest implements AbstractMessage {


    private static final long serialVersionUID = -4304681519884126007L;
    private String xid;
    private Boolean trunk;
    private String applicationId;
    private String clientId;
}
