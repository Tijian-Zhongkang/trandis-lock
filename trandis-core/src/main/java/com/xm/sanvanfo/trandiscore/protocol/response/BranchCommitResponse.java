package com.xm.sanvanfo.trandiscore.protocol.response;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_BRANCH_COMMIT_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchCommitResponse extends BranchAbstractResponse {

    private static final long serialVersionUID = -2303913308800614968L;

    @Override
    public MessageType getMessageType() {
        return TYPE_BRANCH_COMMIT_MSG_RESPONSE;
    }
}
