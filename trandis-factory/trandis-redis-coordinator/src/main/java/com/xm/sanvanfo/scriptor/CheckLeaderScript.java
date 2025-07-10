package com.xm.sanvanfo.scriptor;

public class CheckLeaderScript implements IScript {

    private final static String script = "local list = redis.call('HMGET', KEYS[1], ARGV[1], ARGV[2])\n" +
            "if list == nil\n" +
            "then\n" +
            "  return nil\n" +
            "end\n" +
            "local table = {}\n" +
            "for i=1, #list do\n" +
            "  if list[i] ~= false\n" +
            "     then\n" +
            "     table[ARGV[i]] = list[i]\n" +
            "  end\n" +
            "end\n" +
            "if table[\"content\"] == nil\n" +
            "then\n" +
            "  return nil\n" +
            "end\n" +
            "if table[\"activeTime\"] == nil or ARGV[3] - table[\"activeTime\"] >= 0\n" +
            "then\n" +
            "  redis.call('DEL', KEYS[1])\n" +
            "  return nil\n" +
            "end\n" +
            "return table[\"content\"]";

    @Override
    public String script() {
        return script;
    }
}
