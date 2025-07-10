package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_HEARTBEAT_MSG_RESPONSE;

@Data
public class HeartbeatMessageResponse implements AbstractMessage {


    private static final long serialVersionUID = 630397789497838102L;
    private String serverId;

    @Override
    public MessageType getMessageType() {
        return TYPE_HEARTBEAT_MSG_RESPONSE;
    }
}
