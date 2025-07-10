package com.xm.sanvanfo.trandiscore.session;

public interface SessionStore {

    void store(GlobalSession session);

    void delete(String xid);

    GlobalSession load(String xid);

    default boolean sync() {return false;}
}
