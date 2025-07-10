package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.protocol.MergeMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import lombok.Data;

import java.util.List;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_MERGE_MSG_RESPONSE;

@Data
public class MergeMessageResponse implements AbstractMessage, MergeMessage {

    private static final long serialVersionUID = 4751867877986210189L;
    private List<Integer> ids;
    private List<MessageTypeAware> results;

    @Override
    public MessageType getMessageType() {
        return TYPE_MERGE_MSG_RESPONSE;
    }
}
