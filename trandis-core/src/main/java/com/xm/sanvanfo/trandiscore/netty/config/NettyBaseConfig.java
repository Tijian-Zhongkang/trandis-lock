package com.xm.sanvanfo.trandiscore.netty.config;

import com.xm.sanvanfo.trandiscore.netty.constrant.TransportProtocolType;
import com.xm.sanvanfo.trandiscore.netty.constrant.TransportServerType;
import io.netty.channel.Channel;
import io.netty.channel.ServerChannel;
import lombok.Data;

@Data
public class NettyBaseConfig {

    /**
     * The constant BOSS_THREAD_PREFIX.
     */
    private String BOSS_THREAD_PREFIX;

    /**
     * The constant WORKER_THREAD_PREFIX.
     */
    private String WORKER_THREAD_PREFIX;

    /**
     * The constant SHARE_BOSS_WORKER.
     */
    private boolean SHARE_BOSS_WORKER;

    /**
     * The constant WORKER_THREAD_SIZE.
     */
    private int WORKER_THREAD_SIZE;

    /**
     * The constant TRANSPORT_SERVER_TYPE.
     */
    private TransportServerType TRANSPORT_SERVER_TYPE;


    /**
     * The constant TRANSPORT_PROTOCOL_TYPE.
     */
    private TransportProtocolType TRANSPORT_PROTOCOL_TYPE;

    private int DEFAULT_WRITE_IDLE_SECONDS = 5;

    private int READIDLE_BASE_WRITEIDLE = 3;


    /**
     * The constant MAX_WRITE_IDLE_SECONDS.
     */
    private int MAX_WRITE_IDLE_SECONDS;


    public int getMAX_READ_IDLE_SECONDS() {
        return  MAX_WRITE_IDLE_SECONDS * 3;
    }

    /**
     * The constant MAX_ALL_IDLE_SECONDS.
     */
    private int MAX_ALL_IDLE_SECONDS = 0;

    private  int MAX_NOT_WRITEABLE_RETRY = 2000;

    private byte CONFIGURED_CODEC;
    private byte CONFIGURED_COMPRESSOR;

    private String networkCardName;
    private Integer ipType;

}
