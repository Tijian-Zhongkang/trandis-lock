package com.xm.sanvanfo;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.common.utils.EnvUtils;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import io.netty.bootstrap.ServerBootstrap;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

@SuppressWarnings({"unused"})
@Slf4j
@CustomPlugin(registerClass = IServerBootstrapPlugin.class, name = "redisCoordinator")
public class RedisCoordinatorServerPlugin implements IServerBootstrapPlugin, IPlugin {

    @Override
    public int getOrderBy() {
        return 1;
    }

    @Override
    public void doBindCompleted(ServerBootstrap bootstrap, NettyServerConfig config) {
        String ip = Objects.requireNonNull(EnvUtils.getIpAddress(config.getNetworkCardName(), config.getIpType())).getHostAddress();
         LockCoordinator.INSTANCE().setBootstrapServerInfo(config.getServerAppName(), ip, config.getListenPort());

    }
}
