package com.xm.sanvanfo.scriptor;


public class UpdateMasterInfoScript implements IScript {

    private static final String script = "local master = redis.call('HGET', KEYS[1], ARGV[1])\n" +
            "if (master == nil or master == false or master ~= ARGV[2])\n" +
            "then\n" +
            "  return 1\n" +
            "end\n" +
            "if (ARGV[5] ~= nil)\n" +
            "then\n" +
            "  redis.call('HMSET', KEYS[1], ARGV[3], ARGV[4], ARGV[5], ARGV[6])\n" +
            "else\n" +
            "  redis.call('HSET', KEYS[1], ARGV[3], ARGV[4])\n" +
            "end\n" +
            "redis.call('PEXPIRE', KEYS[1], ARGV[7])\n" +
            "return 0";

    @Override
    public String script() {
        return script;
    }
}
