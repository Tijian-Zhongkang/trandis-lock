package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.GetLockBusResponse;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.request.PluginRequest;

import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
class FollowerGetLockBusRequestProcessor extends AbstractFollowerRequestProcessor {

    FollowerGetLockBusRequestProcessor(IRole role) {
        super(role);
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage rpcMessage, CoordinatorMessageWare body) {
        List<RpcMessage> rpcMessages = follower.getClient().getAllLockedMessages();
        GetLockBusResponse response = new GetLockBusResponse();
        response.setAppName(follower.getConfig().getAppName());
        response.setCoordinatorId(body.getCoordinatorId());
        response.setCode(200);
        response.setMsg("");
        response.setId(body.getId());
        response.setAcquireRequests(new HashSet<>());
        response.setReleaseRequests(new HashSet<>());
        response.setCoordinatorId(follower.getId());
        for (RpcMessage message:rpcMessages
        ) {
            Object obj = message.getBody();
            Asserts.isTrue(obj instanceof PluginRequest);
            Object lock = ((PluginRequest) obj).getBodyObj();
            if(lock instanceof AcquireRequest) {
                if(((AcquireRequest) lock).getDestCoordinatorId().equals(follower.getClient().getBootstrapServerInfo().getId())
                ) {
                    continue;
                }
                else {
                    ((AcquireRequest) lock).setDestCoordinatorId(follower.getClient().getBootstrapServerInfo().getId());
                }
                GetLockBusResponse.RpcAcquire acquire = new GetLockBusResponse.RpcAcquire();
                setCommonParameters(acquire, message);
                acquire.setAcquireRequest((AcquireRequest)lock);
                response.getAcquireRequests().add(acquire);
            }
            else if(lock instanceof ReleaseRequest) {
                if(((ReleaseRequest) lock).getDestCoordinatorId().equals(follower.getClient().getBootstrapServerInfo().getId())
                ) {
                    continue;
                }
                else {
                    ((ReleaseRequest) lock).setDestCoordinatorId(follower.getClient().getBootstrapServerInfo().getId());
                }
                GetLockBusResponse.RpcRelease release = new GetLockBusResponse.RpcRelease();
                setCommonParameters(release, message);
                release.setReleaseRequest((ReleaseRequest)lock);
                response.getReleaseRequests().add(release);
            }
        }
        response.setWaitList(LockMessageBus.INSTANCE().getAllWaitPath());
        response.setDestCoordinatorId(follower.getClient().getBootstrapServerInfo().getId());
        response.setNodeLockInfo(LockMessageBus.INSTANCE().getLockInfo());

        follower.getClient().sendAsyncResponse(rpcMessage.getMessage(),follower.buildPluginResponse(response, CoordinatorConst.getLockBusResponsePlugin));
        return AbstractRole.ProcessStatus.OK;
    }

    private void setCommonParameters(GetLockBusResponse.RpcCommon rpc, RpcMessage message) {
        rpc.setBodyType(message.getBodyType());
        rpc.setCodec(message.getCodec());
        rpc.setCompressor(message.getCompressor());
        rpc.setId(message.getId());
        rpc.setMessageType(message.getMessageType());
    }
}
