package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.request.CloseRequest;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.closeRequestPlugin;

@CustomPlugin(registerClass = IProcessorPlugin.class, name = closeRequestPlugin)
public class CloseRequestPluginProcessor extends CoordinatorServerProcessorPlugin {
    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof CloseRequest);
        return ((CloseRequest)obj).getCoordinatorId();
    }

    @Override
    public Class<?> getSerializerType() {
        return CloseRequest.class;
    }
}
