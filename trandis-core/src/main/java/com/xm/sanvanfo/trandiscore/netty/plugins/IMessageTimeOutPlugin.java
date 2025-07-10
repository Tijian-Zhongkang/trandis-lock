package com.xm.sanvanfo.trandiscore.netty.plugins;

import com.xm.sanvanfo.trandiscore.netty.MessageFuture;

public interface IMessageTimeOutPlugin {
    void accept(MessageFuture future);
}
