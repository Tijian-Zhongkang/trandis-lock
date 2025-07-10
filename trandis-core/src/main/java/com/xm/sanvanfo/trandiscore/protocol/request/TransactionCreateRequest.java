package com.xm.sanvanfo.trandiscore.protocol.request;


import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.session.GlobalTransactionInfo;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_CREATE_TRANSACTION_MSG_REQUEST;

@Data
public class TransactionCreateRequest implements AbstractMessage {

    private static final long serialVersionUID = 4459567518839047800L;

    private GlobalTransactionInfo transactionInfo;
    private String clientId;
    private String applicationId;

    @Override
    public MessageType getMessageType() {
        return TYPE_CREATE_TRANSACTION_MSG_REQUEST;
    }
}
