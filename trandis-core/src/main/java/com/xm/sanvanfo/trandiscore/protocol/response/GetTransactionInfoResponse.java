package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.session.GlobalTransactionInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_GET_TRANSACTION_INFO_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class GetTransactionInfoResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = 3989709527467849760L;
    private GlobalTransactionInfo info;
    private Boolean trunk;

    @Override
    public MessageType getMessageType() {
        return TYPE_GET_TRANSACTION_INFO_RESPONSE;
    }
}
