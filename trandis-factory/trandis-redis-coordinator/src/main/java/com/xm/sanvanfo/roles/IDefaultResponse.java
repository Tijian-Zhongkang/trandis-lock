package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;

public interface IDefaultResponse {

    default PluginResponse defaultFailAcquireResponse(AbstractRole role, AcquireRequest acquireRequest, String message) {
        AcquireResponse acquireResponse = new AcquireResponse();
        acquireResponse.setThreadId(acquireRequest.getThreadId());
        acquireResponse.setAppName(acquireRequest.getAppName());
        acquireResponse.setDestCoordinatorId(acquireRequest.getCoordinatorId());
        acquireResponse.setId(acquireRequest.getId());
        acquireResponse.setCode(BaseResponse.ResponseCode.INNER_ERROR.getCode());
        acquireResponse.setMsg(message);
        acquireResponse.setAcquireRequest(acquireRequest);
        return role.buildPluginResponse(acquireResponse, CoordinatorConst.acquireResponsePlugin);
    }

    default PluginResponse defaultFailReleaseResponse(AbstractRole role,ReleaseRequest releaseRequest, String message) {
        ReleaseResponse releaseResponse = new ReleaseResponse();
        releaseResponse.setThreadId(releaseRequest.getThreadId());
        releaseResponse.setAppName(releaseRequest.getAppName());
        releaseResponse.setDestCoordinatorId(releaseRequest.getCoordinatorId());
        releaseResponse.setId(releaseRequest.getId());
        releaseResponse.setCode(BaseResponse.ResponseCode.INNER_ERROR.getCode());
        releaseResponse.setMsg(message);
        return role.buildPluginResponse(releaseResponse, CoordinatorConst.releaseResponsePlugin);
    }
}
