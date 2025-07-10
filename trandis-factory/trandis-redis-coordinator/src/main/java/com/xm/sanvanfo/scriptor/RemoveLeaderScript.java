package com.xm.sanvanfo.scriptor;

public class RemoveLeaderScript  implements IScript {

    private static final String script = "local master = redis.call('HGET', KEYS[1], ARGV[1])\n" +
            "if (master == nil or master == false or master ~= ARGV[2])\n" +
            "then\n" +
            "  return 0\n" +
            "else\n" +
            "  redis.call('DEL', KEYS[1])\n" +
            "  return 1\n" +
            "end\n";
    @Override
    public String script() {
        return script;
    }
}
