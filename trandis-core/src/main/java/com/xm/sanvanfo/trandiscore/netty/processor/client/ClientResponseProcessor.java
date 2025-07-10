package com.xm.sanvanfo.trandiscore.netty.processor.client;

import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingClient;
import com.xm.sanvanfo.trandiscore.netty.processor.ResponseProcessor;

import java.util.concurrent.ExecutorService;

public class ClientResponseProcessor extends ResponseProcessor {

    public ClientResponseProcessor(AbstractNettyRemotingClient remoting) {
        super(remoting,null);
    }
}
