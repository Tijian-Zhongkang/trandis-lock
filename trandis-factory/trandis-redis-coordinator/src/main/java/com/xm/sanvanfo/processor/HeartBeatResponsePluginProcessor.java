package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.protocol.response.HeartBeatResponse;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.heartBeatResponsePlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = heartBeatResponsePlugin)
public class HeartBeatResponsePluginProcessor extends CoordinatorClientProcessorPlugin {
    @Override
    public Class<?> getSerializerType() {
        return HeartBeatResponse.class;
    }
}
