package com.xm.sanvanfo.trandiscore.protocol.request;


import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_HEARTBEAT_MSG_REQUEST;

@Data
public class HeartbeatMessageRequest implements AbstractMessage {


    private static final long serialVersionUID = -5269824346754589348L;
    private String applicationId;
    private String clientId;

    @Override
    public MessageType getMessageType() {
        return TYPE_HEARTBEAT_MSG_REQUEST;
    }
}
