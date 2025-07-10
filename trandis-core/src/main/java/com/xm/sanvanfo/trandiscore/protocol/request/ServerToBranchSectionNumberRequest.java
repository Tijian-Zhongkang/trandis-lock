package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_REQUEST;

@Data
public class ServerToBranchSectionNumberRequest implements AbstractMessage {

    private static final long serialVersionUID = 4972142364633904722L;
    private String xid;
    private Integer sectionId;
    private String serverId;

    @Override
    public MessageType getMessageType() {
        return TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_REQUEST;
    }
}
