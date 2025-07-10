package com.xm.sanvanfo.trandiscore.protocol.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_GET_TRANSACTION_LOCKS_REQUEST;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class GetTransactionLocksRequest  extends  BranchAbstractRequest {

    private static final long serialVersionUID = 3574936887848266603L;

    @Override
    public MessageType getMessageType() {
        return TYPE_GET_TRANSACTION_LOCKS_REQUEST;
    }
}
