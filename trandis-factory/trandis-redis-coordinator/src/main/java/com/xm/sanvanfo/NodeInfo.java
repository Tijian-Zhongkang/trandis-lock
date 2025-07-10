package com.xm.sanvanfo;

import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import lombok.Data;

@Data
public class NodeInfo {
    private BootstrapServerInfo bootstrapServerInfo;
    private AbstractNettyRemoting remoting;
    private String serverId;
}
