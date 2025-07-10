package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.constants.CoordinatorConst;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

interface ICommonScriptParameterParser extends LockPathSplitter{

    default List<Object> prepareCommonArgvs(Leader role, CoordinatorThreadMessageWare body) {
        List<Object> list = new ArrayList<>();
        list.add(CoordinatorConst.idStr);
        list.add(role.getId());
        list.add(Optional.ofNullable(body.getCoordinatorId()).orElse("") + "-" + body.getThreadId());
        addCommonLockParameter(list, role.getConfig());
        return list;
    }

    default List<Object> prepareCommonKeys(Leader role, CoordinatorMessageWare body) {
        List<Object> list = new ArrayList<>();
        list.add(role.getConfig().getLeaderKey());
        list.add(Optional.ofNullable(body.getCoordinatorId()).orElse("") + "-" + role.getConfig().getLockWaitSuffix());
        list.add(Optional.ofNullable(body.getCoordinatorId()).orElse("") + "-" + role.getConfig().getLockListSuffix());
        return list;
    }

    default List<NodeLockInfo.LockInfo> convertRedisArr(List list) {
        List<NodeLockInfo.LockInfo> lockInfoList = new ArrayList<>();
        int size = list.size();
        for(int i = 0; i < size; i+=2) {
            lockInfoList.add(new NodeLockInfo.LockInfo(list.get(i).toString(), Integer.parseInt(list.get(i + 1).toString())));
        }
        return lockInfoList;
    }
}
