package com.xm.sanvanfo.trandiscore.netty.constrant;

public enum TransportServerType {

    NATIVE("native"),

    NIO("nio");

    public final String name;

    TransportServerType(String name) {
        this.name = name;
    }

    public static TransportServerType getType(String name) {
        for (TransportServerType b : TransportServerType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown type:" + name);
    }
}
