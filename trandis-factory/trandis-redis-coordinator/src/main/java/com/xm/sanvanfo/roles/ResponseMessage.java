package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
class ResponseMessage {
    private RpcMessage rpcRequestMessage;
    private PluginResponse response;
}
