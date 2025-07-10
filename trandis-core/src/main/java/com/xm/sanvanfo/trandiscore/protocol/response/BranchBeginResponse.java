package com.xm.sanvanfo.trandiscore.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_BRANCH_BEGIN_MSG_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class BranchBeginResponse extends BranchAbstractResponse {


    private static final long serialVersionUID = -9165370791925954503L;
    private Integer sectionNumber;

    @Override
    public MessageType getMessageType() {
        return TYPE_BRANCH_BEGIN_MSG_RESPONSE;
    }
}
