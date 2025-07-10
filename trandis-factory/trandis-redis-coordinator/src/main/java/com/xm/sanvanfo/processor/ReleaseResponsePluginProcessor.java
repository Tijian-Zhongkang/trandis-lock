package com.xm.sanvanfo.processor;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.trandiscore.netty.processor.IProcessorPlugin;

import static com.xm.sanvanfo.constants.CoordinatorConst.releaseResponsePlugin;

@SuppressWarnings("unused")
@CustomPlugin(registerClass = IProcessorPlugin.class, name = releaseResponsePlugin)
public class ReleaseResponsePluginProcessor extends AcquireResponsePluginProcessor {

    @Override
    public Class<?> getSerializerType() {
        return ReleaseResponse.class;
    }


}
