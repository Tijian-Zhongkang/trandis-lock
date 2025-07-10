package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_SERVER_TO_BRANCH_COMMIT_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class ServerToBranchCommitResponse extends BranchAbstractResponse {

    private static final long serialVersionUID = 206694940315175359L;


    @Override
    public MessageType getMessageType() {
        return TYPE_SERVER_TO_BRANCH_COMMIT_MSG_RESPONSE;
    }
}
