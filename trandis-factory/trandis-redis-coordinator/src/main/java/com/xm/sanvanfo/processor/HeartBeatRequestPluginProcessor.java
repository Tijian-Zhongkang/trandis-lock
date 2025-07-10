package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.request.HeartBeatRequest;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.heartBeatRequestPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = heartBeatRequestPlugin)
public class HeartBeatRequestPluginProcessor extends CoordinatorServerProcessorPlugin {
    @Override
    public Class<?> getSerializerType() {
        return HeartBeatRequest.class;
    }

    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof HeartBeatRequest);
        return ((HeartBeatRequest)obj).getCoordinatorId();
    }
}
