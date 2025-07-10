package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_NOT_FOND_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class NotFountProcessorResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = -6177889904171625360L;

    private  Integer type;

    @Override
    public MessageType getMessageType() {
        return TYPE_NOT_FOND_MSG_RESPONSE;
    }
}
