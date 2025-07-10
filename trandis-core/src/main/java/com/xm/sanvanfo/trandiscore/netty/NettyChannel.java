package com.xm.sanvanfo.trandiscore.netty;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NettyChannel {
    private NettyPoolKey key;
    private Channel channel;
}
