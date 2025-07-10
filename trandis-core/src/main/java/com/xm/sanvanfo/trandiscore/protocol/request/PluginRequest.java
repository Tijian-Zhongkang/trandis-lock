package com.xm.sanvanfo.trandiscore.protocol.request;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.protocol.IPluginMessage;
import lombok.Data;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_REQUEST;

@Data
public class PluginRequest implements AbstractMessage, IPluginMessage {
    private static final long serialVersionUID = -4018887510141040856L;
    private byte[] objBytes;
    private String plugin;
    private transient Object obj;

    @Override
    public MessageType getMessageType() {
        return TYPE_PLUGIN_REQUEST;
    }

    @Override
    public String getPluginName() {
        return plugin;
    }

    @Override
    public Object getBodyObj() {
        return obj;
    }

}
