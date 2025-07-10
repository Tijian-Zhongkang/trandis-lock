package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;

public interface ILeaderRequestAsyncProcessor extends IRequestAsyncProcessor {

    default void asyncProcessException(Leader leader, CoordinatorMessage message, Throwable ex) {
        boolean ret = leader.checkCoordinatorMessageDrop(message, ex, "async process");
        if(ret) {
            leader.dropFuture(message);
        }
        else {
            ret = leader.init();
            if(!ret) {
                long time = (long) Math.pow(2, message.getFailTimes());
                try {
                    Thread.sleep(time * 1000);
                } catch (InterruptedException ignore) {
                }
            }
            message.setFailTimes(message.getFailTimes() + 1);
            leader.getCoordinator().pushMessage(message);
        }
    }
}
