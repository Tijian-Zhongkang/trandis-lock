package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.NotifyResponse;
import com.xm.sanvanfo.roles.IRole;
import com.xm.sanvanfo.roles.Leader;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemoting;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.xm.sanvanfo.constants.CoordinatorConst.notifyResponsePlugin;

@SuppressWarnings("unused")
@Slf4j
@CustomPlugin(registerClass = IProcessorPlugin.class, name = notifyResponsePlugin)
public class NotifyResponsePluginProcessor extends CoordinatorServerProcessorPlugin {

    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof NotifyResponse);
        return ((NotifyResponse)obj).getCoordinatorId();
    }

    @Override
    public Class<?> getSerializerType() {
        return NotifyResponse.class;
    }

    @Override
    public void process(AbstractNettyRemoting remoting, ChannelHandlerContext ctx, RpcMessage rpcMessage)  {
        remoting.processMessageResult(rpcMessage);
        IRole currentRole = LockCoordinator.INSTANCE().getCurrentRole();
        if(!currentRole.getRole().equals(IRole.RoleType.Leader)) {
            log.debug("role is not leader");
            return;
        }
        Object body = rpcMessage.getBody();
        Asserts.isTrue(body instanceof PluginResponse);
        PluginResponse response = (PluginResponse)body;
        Object pluginBody = response.getBodyObj();
        Asserts.isTrue(pluginBody instanceof NotifyResponse);
        NotifyResponse notifyResponse = (NotifyResponse)pluginBody;
        if(notifyResponse.getCode().equals(BaseResponse.ResponseCode.TIMEOUT.getCode())) {
            ((Leader) currentRole).getLockParser().sendNextNotifyRequest(notifyResponse.getRequest());
        }
    }
}
