package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import lombok.Data;


@Data
public class BranchSection {
    private String xid;
    private Integer sectionNumber;
    private NettyPoolKey clientKey;
    private Integer commitLevel;
    private Boolean end;
}
