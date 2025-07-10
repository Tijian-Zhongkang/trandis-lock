package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.LockerConvertFuture;
import com.xm.sanvanfo.common.SupplierException;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.protocol.response.ReleaseResponse;
import com.xm.sanvanfo.scriptor.ReleaseScript;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Slf4j
class LeaderReleaseRequestProcessor extends AbstractLeaderRequestProcessor implements IDefaultResponse,ICommonScriptParameterParser,ILeaderRequestAsyncProcessor {

    private final ReleaseScript releaseScript;
    LeaderReleaseRequestProcessor(IRole role) {
        super(role);
        releaseScript = new ReleaseScript();
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare body) throws Exception {
        return processRelease(message, (ReleaseRequest)body, true);
    }

    @Override
    public AbstractRole.ProcessStatus processAsync(CoordinatorMessage message, CoordinatorMessageWare body) {
        processReleaseInnerAsync(message, (ReleaseRequest) body);
        return AbstractRole.ProcessStatus.OK;
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        return defaultFailReleaseResponse(leader, (ReleaseRequest) messageWare, message);
    }

    private AbstractRole.ProcessStatus processReleaseResponse(CoordinatorMessage message, ReleaseRequest body, Boolean addRepair,
                                                              SupplierException<ReleaseResponse> responseFunc) throws Exception {
        return  followerLockProcess(()->{
            ReleaseResponse releaseResponse = responseFunc.get();
            if (releaseResponse.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode())) {
                return AbstractRole.ProcessStatus.REPROCESSHEAD;
            }

            if(releaseResponse.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                if(addRepair) {
                    log.warn("add retry task processRelease data: {}", message.toString());
                    leader.getRepairRetryManager().addThreadRetryTask(body.getThreadId(), message, ()->{
                        try {
                            return !AbstractRole.ProcessStatus.REPAIRING.equals(processRelease(message, body, false));
                        }
                        catch (Exception ex) {
                            boolean ret =  leader.checkCoordinatorMessageDrop(message, ex, "repair");
                            if(ret) {
                                leader.dropFuture(message);
                            }
                            return ret;
                        }
                    });
                }
                return AbstractRole.ProcessStatus.REPAIRING;
            }

            asyncSendResponse(message, body, releaseResponse, CoordinatorConst.releaseResponsePlugin);
            return AbstractRole.ProcessStatus.OK;
        }, info -> addRepair && null != info &&  leader.getRepairRetryManager().hasThreadRetryTask(body.getThreadId()),
                body, AbstractRole.ProcessStatus.SKIP, AbstractRole.ProcessStatus.REPROCESSTAIL);
    }

    private AbstractRole.ProcessStatus processRelease(CoordinatorMessage message, ReleaseRequest body, Boolean addRepair) throws Exception {
        return processReleaseResponse(message, body, addRepair, ()->processReleaseInner(body));
    }

    private void processReleaseInnerAsync(CoordinatorMessage message, ReleaseRequest body) {
        ReleaseResponse releaseResponse = new ReleaseResponse();
        if(returnRepairingResponse(body, releaseResponse)) {
            processReleaseResponseAsync(message, body, releaseResponse);
            return;
        }
        List<Object> keys = prepareReleaseKeys(body);
        List<Object> argvs = prepareReleaseArgvs(body);
        try {
            LockerConvertFuture<List> future = leader.getLocker().execScriptAsync(releaseScript.script(), List.class, null, keys, argvs);
            future.addConsumerListener((obj, ex)-> {
                if(null != ex) {
                    asyncProcessException(leader, message, ex);
                }
                else {
                    parseReleaseList((List)obj, releaseResponse);
                    processReleaseResponseAsync(message, body, releaseResponse);
                }
            });
        }
        catch (Exception ex) {
            asyncProcessException(leader, message, ex);
        }
    }

    private void parseReleaseList(List notified, ReleaseResponse releaseResponse) {
        Long result = Long.parseLong(notified.get(0).toString());
        if(result.equals(-1L)) {
            //release fail leader changed
            releaseResponse.setCode(BaseResponse.ResponseCode.REDIRECT.getCode());
            releaseResponse.setMsg("release fail leader changed");
            leader.setNeedReElectionTrue();
        }
        else if(result.equals(0L)) {
            releaseResponse.setCode(BaseResponse.ResponseCode.INNER_ERROR.getCode());
            releaseResponse.setMsg(notified.get(1).toString());
            processReleaseResult(releaseResponse,  notified);
        }
        else {
            releaseResponse.setCode(BaseResponse.ResponseCode.SUCCESS.getCode());
            releaseResponse.setMsg("");
            //release success
            processReleaseResult(releaseResponse, notified);
        }
    }

    private void processReleaseResponseAsync(CoordinatorMessage message, ReleaseRequest body, ReleaseResponse releaseResponse) {
        try {
            AbstractRole.ProcessStatus status = processReleaseResponse(message, body, true, ()->releaseResponse);
            if(status.equals(AbstractRole.ProcessStatus.REPROCESSTAIL)) {
                leader.getCoordinator().pushMessage(message);
            }
            else if(status.equals(AbstractRole.ProcessStatus.REPROCESSHEAD)) {
                leader.getCoordinator().pushMessageFirst(message);
            }
        }
        catch (Exception ex) {
            asyncProcessException(leader, message, ex);
        }
    }

    private boolean returnRepairingResponse(ReleaseRequest body, ReleaseResponse releaseResponse) {
        releaseResponse.setId(body.getId());
        releaseResponse.setCoordinatorId(leader.getId());
        releaseResponse.setAppName(leader.getConfig().getAppName());
        releaseResponse.setDestCoordinatorId(body.getCoordinatorId());
        releaseResponse.setThreadId(body.getThreadId());
        if(leader.getLockRepairManager().isRepairing(body)) {
            releaseResponse.setCode(BaseResponse.ResponseCode.REPAIRING.getCode());
            releaseResponse.setMsg("release fail path is repairing");
            return true;
        }
        return false;
    }

    ReleaseResponse processReleaseInner(ReleaseRequest body) throws Exception {
        ReleaseResponse releaseResponse = new ReleaseResponse();
        if(returnRepairingResponse(body, releaseResponse)) {
            return releaseResponse;
        }
        List<Object> keys = prepareReleaseKeys(body);
        List<Object> argvs = prepareReleaseArgvs(body);
        List notified = leader.getLocker().execScript(releaseScript.script(), List.class, null,  keys, argvs);
        parseReleaseList(notified, releaseResponse);
        return releaseResponse;
    }

    private void processReleaseResult(ReleaseResponse releaseResponse,  List notified) {
        if(notified.size() > 4) {
            int notifySize = Integer.parseInt(notified.get(2).toString());
            int releaseSize = Integer.parseInt(notified.get(3).toString());
            List notifiedList = notified.subList(5, 5 + notifySize);
            if(notified.size() > 5 + notifySize) {
                List releaseData = notified.subList(5 + notifySize,   5 + notifySize + releaseSize * 2);
                releaseResponse.setReleasePaths(convertRedisArr(releaseData));
            }
            if(notified.size() > 5 + notifySize + releaseSize) {
                List releaseFailData = notified.subList(5 + notifySize + releaseSize * 2, notified.size());
                releaseResponse.setFailPaths(convertRedisArr(releaseFailData));
            }
            boolean ret = leader.getLockRepairManager().releaseLock(releaseResponse, notifiedList);
            if(ret) {
                releaseResponse.setCode(BaseResponse.ResponseCode.REPAIRING.getCode());
                releaseResponse.setMsg("release repair checking return true");
            }
            LockParser lockParser = leader.getLockParser();
            lockParser.notifyWaitKeys(leader.getLockRepairManager().notifyLock(lockParser.filterNotInit(notifiedList)));
        }
    }



    private List<Object> prepareReleaseArgvs(ReleaseRequest body) {
        List<Object> list = prepareCommonArgvs(leader, body);
        List<Object> readWrite = new ArrayList<>();
        List<Object> reEnter = new ArrayList<>();
        if(null != body.getWritePath()) {
            for (String ignored : body.getWritePath()
            ) {
                readWrite.add(1);
                reEnter.add(body.getWriteEntrantTimes());
            }
        }
        if(null != body.getReadPath()) {
            for (String ignored : body.getReadPath()
            ) {
                readWrite.add(0);
                reEnter.add(body.getReadEntrantTimes());
            }
        }
        list.addAll(readWrite);
        list.addAll(reEnter);
        return list;
    }

    private List<Object> prepareReleaseKeys(ReleaseRequest body) {
        List<Object> list = prepareCommonKeys(leader, body);
        if(null != body.getWritePath()) {
            list.addAll(body.getWritePath());
        }
        if(null != body.getReadPath()) {
            list.addAll(body.getReadPath());
        }
        return list;
    }

}
