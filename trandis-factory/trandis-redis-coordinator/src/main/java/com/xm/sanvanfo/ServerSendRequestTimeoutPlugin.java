package com.xm.sanvanfo;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.roles.IRole;
import com.xm.sanvanfo.roles.Leader;
import com.xm.sanvanfo.trandiscore.netty.MessageFuture;
import com.xm.sanvanfo.trandiscore.netty.plugins.IMessageTimeOutPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IMessageTimeOutPlugin.class, name = "redisCoordinatorServerTimeout")
public class ServerSendRequestTimeoutPlugin implements IMessageTimeOutPlugin, IPlugin {

    @Override
    public void accept(MessageFuture future) {
        IRole role = LockCoordinator.INSTANCE().getCurrentRole();
        if(role instanceof Leader) {
            ((Leader)role).sendRequestTimeout(future);
        }
    }
}
