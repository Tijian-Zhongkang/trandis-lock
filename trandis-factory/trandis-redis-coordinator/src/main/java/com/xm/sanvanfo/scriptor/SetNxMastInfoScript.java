package com.xm.sanvanfo.scriptor;


public class SetNxMastInfoScript implements IScript {


    private static final String script = "local val = redis.call('EXISTS', KEYS[1])\n" +
            "if (val == 0)\n" +
            "then\n" +
            "  redis.call('HMSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4], ARGV[5], ARGV[6])\n" +
            "  redis.call('PEXPIRE', KEYS[1], ARGV[7])\n" +
            "  redis.call('DEL', KEYS[2])\n" +
            "end\n" +
            "if (val == 1)\n" +
            "then\n" +
            "  return 0\n" +
            "else\n" +
            "  return 1\n" +
            "end";
    @Override
    public String script() {
        return script;
    }

}
