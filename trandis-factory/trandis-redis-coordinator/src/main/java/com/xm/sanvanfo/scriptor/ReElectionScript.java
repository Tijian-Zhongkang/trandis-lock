package com.xm.sanvanfo.scriptor;

public class ReElectionScript implements IScript {

    private static final String script = "local master = redis.call('HMGET', KEYS[3], ARGV[3], ARGV[5])\n" +
            "if (master[1] ~= nil and master[1] ~= false and master[1] ~= ARGV[4]) or (master[2]~= nil and master[2] ~= false and master[2] > ARGV[6]) \n" +
            "then\n" +
            "  return 0\n" +
            "end\n" +
            "redis.call('SADD', KEYS[1], ARGV[1])\n" +
            "local reElection = redis.call('SCARD', KEYS[1])\n" +
            "local nodes = redis.call('SMEMBERS', KEYS[2])\n" +
            "local table = {}\n" +
            "local nodeSize = 0\n" +
            "for i=1, #nodes do\n" +
            "  local nodeId = string.sub(nodes[i], 1, string.find(nodes[i], '-') - 1)\n" +
            "  if table[nodeId] == nil\n" +
            "   then\n" +
            "     table[nodeId] = '1'\n" +
            "     nodeSize = nodeSize + 1\n" +
            "  end\n" +
            "end\n" +
            "if (nodeSize <= 2 or (nodeSize > 2 and reElection > nodeSize / 2))\n" +
            "then\n" +
            "  redis.call('DEL', KEYS[3])\n" +
            "  redis.call('PERSIST', KEYS[1])\n" +
            "  return 1\n" +
            "else\n" +
            "  redis.call('PEXPIRE', KEYS[1], ARGV[2])\n" +
            "  return 0\n" +
            "end";

    @Override
    public String script() {
        // must check new leader or leader updated,election node is ip:port
        return  script;

    }
}
