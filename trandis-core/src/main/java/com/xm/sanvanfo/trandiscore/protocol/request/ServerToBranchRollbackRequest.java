package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_REQUEST;

@Data
public class ServerToBranchRollbackRequest implements AbstractMessage {

    private static final long serialVersionUID = 5839773367232833629L;
    private String xid;
    private Integer sectionId;
    private String serverId;

    @Override
    public MessageType getMessageType() {
        return TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_REQUEST;
    }
}
