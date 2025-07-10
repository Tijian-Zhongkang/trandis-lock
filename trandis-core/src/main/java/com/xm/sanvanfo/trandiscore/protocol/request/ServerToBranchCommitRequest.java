package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_SERVER_TO_BRANCH_COMMIT_MSG_REQUEST;

//server to client

@Data
public class ServerToBranchCommitRequest implements AbstractMessage {

    private static final long serialVersionUID = 3252887304844680424L;
    private String xid;
    private String serverId;

    @Override
    public MessageType getMessageType() {
        return TYPE_SERVER_TO_BRANCH_COMMIT_MSG_REQUEST;
    }
}
