package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.releaseRequestPlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = releaseRequestPlugin)
public class ReleaseRequestPluginProcessor extends CoordinatorServerProcessorPlugin {
    @Override
    public Class<?> getSerializerType() {
        return ReleaseRequest.class;
    }

    @Override
    String getCoordinatorId(Object obj) {
        Asserts.isTrue(obj instanceof ReleaseRequest);
        return ((ReleaseRequest)obj).getCoordinatorId();
    }
}
