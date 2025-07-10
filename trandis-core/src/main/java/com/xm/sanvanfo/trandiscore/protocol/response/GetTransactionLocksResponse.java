package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_GET_TRANSACTION_LOCKS_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class GetTransactionLocksResponse extends BranchAbstractResponse {

    private static final long serialVersionUID = 4945676961411051311L;
    private Set<String> locks;
    @Override
    public MessageType getMessageType() {
        return TYPE_GET_TRANSACTION_LOCKS_RESPONSE;
    }
}
