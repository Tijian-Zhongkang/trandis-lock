package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_CREATE_TRANSACTION_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class TransactionCreateResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = -1115471644091431710L;
    private String serverId;

    @Override
    public MessageType getMessageType() {
        return TYPE_CREATE_TRANSACTION_MSG_RESPONSE;
    }
}
