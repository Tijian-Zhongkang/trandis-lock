package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.common.utils.ObjectHolder;
import io.netty.channel.ChannelHandlerContext;

interface IInterProgressParser {

    enum InterProgressType {
        Lock
    }

    void shutdown();

    void initFollowerNode(String node);

    void initLeader();

    void deleteFollower(FollowerInfo followerInfo, String followerId, ObjectHolder<Boolean> needRepair) throws Exception;

    Long close();

    boolean rebuildFollowerFail(ChannelHandlerContext ctx, String id);
}
