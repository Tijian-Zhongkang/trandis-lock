package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BranchAbstractResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = 784418884952806098L;
    private String xid;
    private String applicationId;
    private String serverId;
    private String clientId;
}
