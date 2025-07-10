package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.protocol.MergeMessage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_MERGE_MSG_REQUEST;

@Data
public class MergeMessageRequest implements AbstractMessage, MergeMessage {

    private static final long serialVersionUID = 3023392964102231936L;
    private List<AbstractMessage> msgs;
    private List<Integer> ids;
    private String applicationId;
    private String clientId;

    @Override
    public MessageType getMessageType() {
        return TYPE_MERGE_MSG_REQUEST;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MergeMessageRequest: {\n");
        stringBuilder.append("msgs:[\n");
        Optional.ofNullable(msgs).ifPresent(o->{
            List<String> content = new ArrayList<>();
            o.forEach(p-> content.add(p.toString()));
            stringBuilder.append(String.join(",\n", content));
        });
        stringBuilder.append("\n]\n}");
        return stringBuilder.toString();
    }

}
