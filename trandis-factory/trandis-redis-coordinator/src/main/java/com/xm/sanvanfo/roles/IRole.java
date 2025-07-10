package com.xm.sanvanfo.roles;

import com.xm.sanvanfo.CoordinatorMessage;
import com.xm.sanvanfo.protocol.CoordinatorMessageWare;

import java.util.Deque;


@SuppressWarnings("unused")
public interface IRole {
    enum RoleType {
        Candidate(0),
        Leader(1),
        Follower(2);

        private int code;
        RoleType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static RoleType get(int code) {
            return RoleType.values()[code];
        }
    }

    RoleType checkChange();

    boolean init() throws Exception;

    void process(Deque<CoordinatorMessage> deque);

    void directProcess(CoordinatorMessage message, CoordinatorMessageWare body);

    RoleType getRole();

    default  void shutdown() {}

    //shutdown dont remove locks,but close remove locks
    default void close() {shutdown();}
}
