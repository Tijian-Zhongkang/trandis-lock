package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.*;
import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.request.AcquireRequest;
import com.xm.sanvanfo.protocol.request.ReleaseRequest;
import com.xm.sanvanfo.scriptor.SetNxMastInfoScript;
import com.xm.sanvanfo.trandiscore.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Deque;


@Slf4j
public class Candidate extends AbstractRole {


    private volatile int master;
    private final SetNxMastInfoScript setNxMastInfoScript;
    public Candidate(CoordinatorConfig config, String id, IRedisLocker locker) {
        super(config, id, locker, RoleType.Candidate);
         setNxMastInfoScript = new SetNxMastInfoScript();
        startAliveCheck();
    }

    @Override
    public RoleType checkChange() {
        return 1 == master ? RoleType.Leader : RoleType.Follower;
    }

    @Override
    public boolean init() throws Exception {
        BootstrapServerInfo info = new BootstrapServerInfo();
        info.setId(id);
        info.setActiveTime(Clock.systemDefaultZone().millis());
        info.setStatus(BootstrapServerInfo.Status.INIT);
        master = locker.execScript(setNxMastInfoScript.script(), Integer.class, null,
                setNxMastInfoScript.scriptKey(coordinatorConfig.getLeaderKey(), coordinatorConfig.getElectionKey()),
                setNxMastInfoScript.scriptArgv(CoordinatorConst.contentStr, locker.getSerializer().serializeString(info), CoordinatorConst.idStr,
                        info.getId(), CoordinatorConst.activeTimeStr, info.getActiveTime(), coordinatorConfig.getLeaderKeyTimeout()));

        log.debug("{} reElection {}", id, 1 == master ? "true" : "false");
        return true;
    }

    @Override
    public void process(Deque<CoordinatorMessage> deque) {
        log.debug("state is candidate would not be working");
    }

    @Override
    void aliveCheck() {
        log.debug("candidate is living:{}", id);
    }

    @Override
    boolean roleIsChanged() {
        throw new BusinessException("candidate dos not process message");
    }


}
