package com.xm.sanvanfo.trandiscore.protocol.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_BRANCH_COMMIT_MSG_REQUEST;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchCommitRequest extends BranchAbstractRequest {


    private static final long serialVersionUID = 8807545663566457210L;
    private Integer sectionNumber;

    @Override
    public MessageType getMessageType() {
        return TYPE_BRANCH_COMMIT_MSG_REQUEST;
    }
}
