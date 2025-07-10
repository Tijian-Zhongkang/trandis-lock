package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_GET_TRANSACTION_INFO;

@Data
public class GetTransactionInfoRequest implements AbstractMessage {

    private static final long serialVersionUID = -4982856635411240583L;
    private String xid;
    private String applicationId;
    private String clientId;
    @Override
    public MessageType getMessageType() {
        return TYPE_GET_TRANSACTION_INFO;
    }
}
