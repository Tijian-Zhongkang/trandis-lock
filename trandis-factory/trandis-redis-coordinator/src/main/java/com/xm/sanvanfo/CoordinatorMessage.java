package com.xm.sanvanfo;

import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CoordinatorMessage {

    public CoordinatorMessage(RpcMessage message) {
        this.message = message;
    }

    private RpcMessage message;
    private Integer failTimes = 0;
    private Long addTime = 0L;
}
