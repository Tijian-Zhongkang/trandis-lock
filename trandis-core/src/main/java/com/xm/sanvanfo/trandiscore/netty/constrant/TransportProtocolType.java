package com.xm.sanvanfo.trandiscore.netty.constrant;

public enum TransportProtocolType {

    TCP("tcp"),

    UNIX_DOMAIN_SOCKET("unix-domain-socket");


    public final String name;

    TransportProtocolType(String name) {
        this.name = name;
    }

    public static TransportProtocolType getType(String name) {
        name = name.trim().replace('-', '_');
        for (TransportProtocolType b : TransportProtocolType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown type:" + name);
    }
}
