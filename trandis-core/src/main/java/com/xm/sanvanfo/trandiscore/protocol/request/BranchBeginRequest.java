package com.xm.sanvanfo.trandiscore.protocol.request;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_BRANCH_BEGIN_MSG_REQUEST;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchBeginRequest extends BranchAbstractRequest {


    private static final long serialVersionUID = 439089856830196384L;

    @Override
    public MessageType getMessageType() {
        return TYPE_BRANCH_BEGIN_MSG_REQUEST;
    }
}
