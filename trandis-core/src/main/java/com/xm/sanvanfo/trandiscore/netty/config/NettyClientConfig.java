package com.xm.sanvanfo.trandiscore.netty.config;

import com.xm.sanvanfo.trandiscore.netty.balancer.Balancer;
import com.xm.sanvanfo.trandiscore.netty.balancer.invoker.Invoker;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NettyClientConfig extends NettyBaseConfig {

    private int connectTimeoutMillis = 10000;
    private int clientSocketSndBufSize = 153600;
    private int clientSocketRcvBufSize = 153600;
    private int clientWorkerThreads = getWORKER_THREAD_SIZE();
    private Class<? extends Channel> clientChannelClazz;
    private int perHostMaxConn = 2;
    private  final int PER_HOST_MIN_CONN = 2;
    private int pendingConnSize = Integer.MAX_VALUE;
    private  final int RPC_REQUEST_TIMEOUT = 500 * 1000;
    private  String clientAppName;
    private  int maxInactiveChannelCheck = 10;
    private  int MAX_CHECK_ALIVE_RETRY = 300;
    private  int CHECK_ALIVE_INTERVAL = 10;
    private  String SOCKET_ADDRESS_START_CHAR = "/";
    private  long MAX_ACQUIRE_CONN_MILLS = 60 * 1000L;
    private  String RPC_DISPATCH_THREAD_PREFIX = "rpcDispatch";
    private  int DEFAULT_MAX_POOL_ACTIVE = 8;
    private  int DEFAULT_MIN_POOL_IDLE = 0;
    private  boolean DEFAULT_POOL_TEST_BORROW = true;
    private  boolean DEFAULT_POOL_TEST_RETURN = true;
    private  boolean DEFAULT_POOL_LIFO = true;
    private  boolean ENABLE_CLIENT_BATCH_SEND_REQUEST;
    private Balancer.BalanceType balanceType;
    private String serverId;
}
