package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.acquireRequestPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = acquireRequestPlugin)
public class AcquireRequestPluginProcessor extends CoordinatorServerProcessorPlugin {

    @Override
    public Class<?> getSerializerType() {
        return AcquireRequest.class;
    }

    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof AcquireRequest);
        return ((AcquireRequest)obj).getCoordinatorId();
    }
}
