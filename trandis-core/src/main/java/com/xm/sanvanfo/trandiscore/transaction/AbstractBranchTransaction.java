package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.GlobalLockKey;
import com.xm.sanvanfo.trandiscore.TransactionInfo;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.protocol.LockStepType;
import com.xm.sanvanfo.trandiscore.protocol.request.BranchAbstractRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
public abstract class AbstractBranchTransaction implements BranchTransaction {

    protected TransactionInfo transactionInfo;

    @Getter
    protected String clientId;

    protected Boolean trunk;
    protected TransactionLevelEx levelEx;

    public AbstractBranchTransaction(TransactionLevelEx levelEx) {
        this.levelEx = levelEx;
    }


    @Override
    public boolean isTrunk() {
        return trunk;
    }


    @Override
    public void setDefaultRequest(BranchAbstractRequest request) {
        request.setXid(transactionInfo.getXid());
        request.setTrunk(trunk);
        request.setApplicationId(getApplicationId());
        request.setClientId(clientId);
    }

    protected abstract String getApplicationId();



    protected TransactionStatusEx getFail(LockStepType type) {
        return type.equals(LockStepType.Begin) ? TransactionStatusEx.BeginFail : TransactionStatusEx.CommitFailed;
    }


    protected void mergeLockKeyMap(Map<Boolean,List<String>> map1, Map<Boolean,List<String>> map2) {
        for (Map.Entry<Boolean,List<String>> entry:map2.entrySet()
        ) {
            if(!map1.containsKey(entry.getKey())) {
                map1.put(entry.getKey(), new ArrayList<>());
            }
            map1.get(entry.getKey()).addAll(entry.getValue());
        }
    }


    protected void mergeLockMap(Map<String, GlobalLockKey> lockKeys, Map<String, GlobalLockKey> branchLocks) {
        for (Map.Entry<String, GlobalLockKey> entry:branchLocks.entrySet()
        ) {
            if(!lockKeys.containsKey(entry.getKey())) {
                lockKeys.put(entry.getKey(), entry.getValue());
            }
            else if(entry.getValue().getWrite()) {
                lockKeys.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
