package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.constants.CoordinatorConst;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface LockPathSplitter {

    @Data
     class LockSplit {
        private String readWrite;
        private String threadId;
        private String coordinatorId;
        private String path = "";
    }

    default LockSplit splitNotifyLockPart(String o) {
        String[] parts = o.split("-");
        LockSplit lockSplit = new LockSplit();
        lockSplit.setReadWrite(parts[parts.length - 1]);        //read or write
        if(parts.length > 3) {
            lockSplit.setThreadId(parts[parts.length - 2]);    //threadId
        }
        if(parts.length > 3) {
            lockSplit.setCoordinatorId(parts[parts.length - 4] + "-" + parts[parts.length - 3]);  //coordinatorId
        }
        List<String> path = parts.length > 4 ? new ArrayList<>(Arrays.asList(parts).subList(0, parts.length - 5)) : new ArrayList<>();
        if(path.size() > 0) {
            lockSplit.setPath(String.join("-", path));  //path
        }

        return lockSplit;
    }

    default String convertToString(List list) {
        List<String> str = new ArrayList<>();
        for (Object o:list
             ) {
            str.add(o.toString());
        }
        return String.join(",", str);
    }

    default void addCommonLockParameter(List<Object> list, CoordinatorConfig coordinatorConfig) {
        list.add(coordinatorConfig.getReadLockSuffix());
        list.add(coordinatorConfig.getWriteLockSuffix());
        list.add(coordinatorConfig.getLockWaitSuffix());
        list.add(coordinatorConfig.getNotifyWaitSuffix());
        list.add(CoordinatorConst.readEnterStr);
        list.add(CoordinatorConst.writeEnterStr);
        list.add(CoordinatorConst.idStr);
    }

}
