package com.xm.sanvanfo.constant;

public enum RedisKeyType {
    String(1),
    Set(2),
    SortedSet(3),
    Hash(4),
    List(5),
    Geo(6),
    Bitmap(7),
    Db(8),
    Ttl(9),
    HyperLogLog(10);

    private int code;

    RedisKeyType(int c) {
        this.code = c;
    }

    public int getCode() {
        return code;
    }

    public static RedisKeyType get(int code) {
        return RedisKeyType.values()[code];
    }
}
