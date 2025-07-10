package com.xm.sanvanfo.bloom;

import com.xm.sanvanfo.constant.RedisConstant;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractBitArray implements BitArray {

    protected String key;

    private long bitSize;

    public AbstractBitArray(String prefix) {
        this.key = redisPrefix()  + prefix;
    }

    @Override
    public void setBitSize(long bitSize) {
        if (bitSize > maxRedisBitSize())
            throw new IllegalArgumentException("Invalid redis bit size, must small than 2 to the max size default 32");

        this.bitSize = bitSize;
    }

    @Override
    public long bitSize() {
        return this.bitSize;
    }

    protected String redisPrefix() {
        return RedisConstant.REDIS_PREFIX;
    }

    protected long maxRedisBitSize() {
        return RedisConstant.MAX_REDIS_BIT_SIZE;
    }

}
