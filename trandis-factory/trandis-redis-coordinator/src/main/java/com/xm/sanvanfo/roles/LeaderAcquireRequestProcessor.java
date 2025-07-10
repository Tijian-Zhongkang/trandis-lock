package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.LockerConvertFuture;
import com.xm.sanvanfo.common.SupplierException;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.protocol.response.AcquireResponse;
import com.xm.sanvanfo.protocol.response.BaseResponse;
import com.xm.sanvanfo.scriptor.AcquireScript;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.protocol.response.PluginResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Slf4j
class LeaderAcquireRequestProcessor extends AbstractLeaderRequestProcessor implements IDefaultResponse,ICommonScriptParameterParser,ILeaderRequestAsyncProcessor {

    private final AcquireScript acquireScript;
    LeaderAcquireRequestProcessor(IRole role) {
        super(role);
        acquireScript = new AcquireScript();
    }

    @Override
    public AbstractRole.ProcessStatus process(CoordinatorMessage message, CoordinatorMessageWare body) throws Exception {
        return processAcquire(message, (AcquireRequest) body, true);
    }

    @Override
    public PluginResponse defaultErrorResponse(CoordinatorMessageWare messageWare, String message){
        return defaultFailAcquireResponse(leader, (AcquireRequest) messageWare, message);
    }

    @Override
    public AbstractRole.ProcessStatus processAsync(CoordinatorMessage message, CoordinatorMessageWare body) {
        processAcquireInnerAsync(message, (AcquireRequest) body);
        return AbstractRole.ProcessStatus.OK;
    }

    private AbstractRole.ProcessStatus processAcquire(CoordinatorMessage message, AcquireRequest body, Boolean addRepair) throws Exception {
        return processResponse(message, body, addRepair, ()-> processAcquireInner(body));
    }

    private AbstractRole.ProcessStatus processResponse(CoordinatorMessage message, AcquireRequest body, Boolean addRepair, SupplierException<AcquireResponse> responseFunc) throws Exception {
        return followerLockProcess( () -> {
            AcquireResponse response = responseFunc.get();
            if (response.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode())) {
                return AbstractRole.ProcessStatus.REPROCESSHEAD;
            }
            if(response.getCode().equals(BaseResponse.ResponseCode.REPAIRING.getCode())) {
                if(addRepair) {
                    log.warn("add retry task processAcquire data: {}", message.toString());
                    leader.getRepairRetryManager().addThreadRetryTask(body.getThreadId() ,message, ()->{
                        try {
                            return !AbstractRole.ProcessStatus.REPAIRING.equals(processAcquire(message, body, false));
                        }
                        catch (Exception ex) {
                            boolean ret = leader.checkCoordinatorMessageDrop(message, ex, "repair");
                            if(ret) {
                                leader.dropFuture(message);
                            }
                            return ret;
                        }
                    });
                }
                return AbstractRole.ProcessStatus.REPAIRING;
            }
            asyncSendResponse(message, body, response, CoordinatorConst.acquireResponsePlugin);
            return AbstractRole.ProcessStatus.OK;
        }, info -> addRepair && null != info &&  leader.getRepairRetryManager().hasThreadRetryTask(body.getThreadId()),
                body, AbstractRole.ProcessStatus.SKIP, AbstractRole.ProcessStatus.REPROCESSTAIL);
    }

    private boolean returnRepairingResponse(AcquireRequest body, AcquireResponse acquireResponse) {
        acquireResponse.setDestCoordinatorId(body.getCoordinatorId());
        acquireResponse.setAcquireRequest(body);
        acquireResponse.setId(body.getId());
        acquireResponse.setAppName(leader.getConfig().getAppName());
        acquireResponse.setCoordinatorId(leader.getId());
        acquireResponse.setThreadId(body.getThreadId());
        if(leader.getLockRepairManager().isRepairing(body)) {
            acquireResponse.setCode(BaseResponse.ResponseCode.REPAIRING.getCode());
            acquireResponse.setMsg("acquire fail path is repairing");
            return true;
        }
        return false;
    }

    private void processAcquireInnerAsync(CoordinatorMessage message, AcquireRequest body) {
        AcquireResponse acquireResponse = new AcquireResponse();
       if(returnRepairingResponse(body, acquireResponse)) {
           processResponseAsync(message, body, acquireResponse);
           return;
       }
        ReleaseRequest releaseRequest = leader.getCoordinator().createFrom(body);
        List<Object> keys = prepareAcquireKeys(body);
        List<Object> argv = prepareAcquireArgv(body);
        try {
            LockerConvertFuture<List> future = leader.getLocker().execScriptAsync(acquireScript.script(), List.class, null, keys, argv);
            future.addConsumerListener((l, ex) -> {
                if(null != ex) {
                    releaseAndExceptionAsync(message, releaseRequest, ex);
                }
                else {
                    parseAcquireList((List)l, acquireResponse);
                    processResponseAsync(message, body, acquireResponse);
                }
            });
        }
        catch (Exception ex) {
            releaseAndExceptionAsync(message, releaseRequest, ex);
        }
    }

    private void releaseAndExceptionAsync(CoordinatorMessage message, ReleaseRequest releaseRequest, Throwable ex) {
        IRequestProcessor processor = leader.get(ReleaseRequest.class);
        Asserts.isTrue(processor instanceof LeaderReleaseRequestProcessor);
        LeaderReleaseRequestProcessor releaseRequestProcessor = (LeaderReleaseRequestProcessor)processor;
        try {
            releaseRequestProcessor.processReleaseInner(releaseRequest);
        }
        catch (Exception reex) {
           log.warn("release error:{}, ex:{}", releaseRequest.toString(), BusinessException.exceptionFullMessage(reex));
        }
        finally {
            asyncProcessException(leader, message, ex);
        }
    }

    private void processResponseAsync(CoordinatorMessage message, AcquireRequest body, AcquireResponse response) {
        try {
            AbstractRole.ProcessStatus status = processResponse(message, body, true, ()->response);
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


    AcquireResponse processAcquireInner(AcquireRequest body) throws Exception {

        AcquireResponse acquireResponse = new AcquireResponse();
        if(returnRepairingResponse(body, acquireResponse)) {
            return acquireResponse;
        }
        ReleaseRequest releaseRequest = leader.getCoordinator().createFrom(body);
        try {
            List<Object> keys = prepareAcquireKeys(body);
            List<Object> argv = prepareAcquireArgv(body);
            List list = leader.getLocker().execScript(acquireScript.script(), List.class, null, keys, argv);
            parseAcquireList(list, acquireResponse);
        }
        catch (Exception ex) {
            IRequestProcessor processor = leader.get(ReleaseRequest.class);
            Asserts.isTrue(processor instanceof LeaderReleaseRequestProcessor);
            LeaderReleaseRequestProcessor releaseRequestProcessor = (LeaderReleaseRequestProcessor)processor;
            releaseRequestProcessor.processReleaseInner(releaseRequest);
            throw ex;
        }
        return acquireResponse;
    }

    private void parseAcquireList(List list, AcquireResponse acquireResponse) {
        List lockDatas = null;
        Long result = Long.parseLong(list.get(0).toString());
        if (result.equals(-1L)) {
            //lock fail leader changed
            acquireResponse.setCode(BaseResponse.ResponseCode.REDIRECT.getCode());
            acquireResponse.setMsg("acquire fail leader changed");
            leader.setNeedReElectionTrue();
        } else if (result.equals(1L)) {
            //lock success
            acquireResponse.setCode(BaseResponse.ResponseCode.SUCCESS.getCode());
            acquireResponse.setMsg("");
            if(list.size() > 4) {
                lockDatas = list.subList(3, list.size());
            }
        } else {
            //lock fail wait other thread release
            acquireResponse.setCode(BaseResponse.ResponseCode.WAITSIGNAL.getCode());
            acquireResponse.setWaitPath( new NodeLockInfo.LockWaitInfo(list.get(2).toString(), Clock.systemDefaultZone().millis()));
            acquireResponse.setMsg(list.get(1).toString());
            if(list.size() > 4) {
                lockDatas = list.subList(3, list.size());

            }
        }
        Optional.ofNullable(lockDatas).ifPresent(o-> acquireResponse.setLockPath(convertRedisArr(o)));
        if(!acquireResponse.getCode().equals(BaseResponse.ResponseCode.REDIRECT.getCode())) {
            boolean ret = leader.getLockRepairManager().acquireLock(acquireResponse);
            if(ret) {
                acquireResponse.setCode(BaseResponse.ResponseCode.REPAIRING.getCode());
                acquireResponse.setMsg("acquire repair checking return true");
            }
        }
    }

    private List<Object> prepareAcquireArgv(AcquireRequest body) {
        List<Object> list = prepareCommonArgvs(leader, body);
        List<Object> readWrite = new ArrayList<>();
        List<Object> reEnter = new ArrayList<>();
        if(null != body.getWritePath() && null != body.getWritePath().getPath()) {
            for (String ignored : body.getWritePath().getPath()
            ) {
                readWrite.add(1);
                reEnter.add(body.getWritePath().getEntrantTimes());
            }
        }
        if(null != body.getReadPath() && null != body.getReadPath().getPath()) {
            for (String ignored : body.getReadPath().getPath()
            ) {
                readWrite.add(0);
                reEnter.add(body.getReadPath().getEntrantTimes());
            }
        }
        list.addAll(readWrite);
        list.addAll(reEnter);
        return list;
    }

    private List<Object> prepareAcquireKeys(AcquireRequest body) {
        List<Object> list = prepareCommonKeys(leader, body);
        if(null != body.getWritePath() && null != body.getWritePath().getPath()) {
            list.addAll(body.getWritePath().getPath());
        }
        if(null!= body.getReadPath() && null != body.getReadPath().getPath()) {
            list.addAll(body.getReadPath().getPath());
        }
        return list;
    }

}
