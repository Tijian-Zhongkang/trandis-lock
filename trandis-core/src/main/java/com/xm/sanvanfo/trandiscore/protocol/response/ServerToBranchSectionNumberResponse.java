package com.xm.sanvanfo.trandiscore.protocol.response;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class ServerToBranchSectionNumberResponse extends BranchAbstractResponse {

    private static final long serialVersionUID = -3419872367085390639L;

    @Override
    public MessageType getMessageType() {
        return TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_RESPONSE;
    }
}
