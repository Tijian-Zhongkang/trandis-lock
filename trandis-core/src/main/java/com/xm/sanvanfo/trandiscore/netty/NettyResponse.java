package com.xm.sanvanfo.trandiscore.netty;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class NettyResponse {
    private RpcMessage rpcMessage;
    private Object msg;
}
