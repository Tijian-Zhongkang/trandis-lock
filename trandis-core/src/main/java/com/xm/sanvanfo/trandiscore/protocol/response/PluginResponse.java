package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.protocol.IPluginMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_PLUGIN_RESPONSE;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class PluginResponse extends AbstractTransactionResponse implements IPluginMessage {
    private static final long serialVersionUID = -1901095163057485628L;

    private String plugin;
    private byte[] objBytes;
    private transient Object obj;

    @Override
    public MessageType getMessageType() {
        return TYPE_PLUGIN_RESPONSE;
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
