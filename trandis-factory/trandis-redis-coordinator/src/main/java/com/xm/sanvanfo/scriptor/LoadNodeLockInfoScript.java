package com.xm.sanvanfo.scriptor;

public class LoadNodeLockInfoScript extends LockScript {

    private final static String script;

    static {
        String checkLeader = checkLeader("result");
        script = lastFlag() +
                getPathAndThreadId() +
                "local result = {}\n" +
                "result[1] = -1\n" +
                "result[2] = \"leader is changed\"" +
                checkLeader +
                "local wait = redis.call('SMEMBERS', KEYS[2])\n" +
                "local lock = redis.call('SMEMBERS', KEYS[3])\n" +
                "result[3] = #lock\n" +
                "result[4] = #wait\n" +
                "local length = 5\n" +
                "local total = #lock + #wait\n" +
                "for i = 1, #lock do\n" +
                "    result[length] = lock[i]\n" +
                "    local flag = lastFlag(lock[i], \"-\")\n" +
                "    local path\n" +
                "    local threadId\n" +
                "    if flag == \"read\"\n" +
                "    then\n" +
                "      local splitArr = getPathAndThreadId(lock[i], -6)\n" +
                "      path = splitArr[2]\n " +
                "      threadId = splitArr[1]\n" +
                "      KEYS[i + 3 +  total] = path..\"-\"..ARGV[6]\n" +
                "      result[length + 1] = redis.call('HGET', KEYS[i + 3 + total], threadId)\n" +
                "      if result[length + 1] == false or result[length + 1] == nil\n" +
                "      then\n" +
                "        result[length + 1] = -1\n" +
                "      end\n" +
                "    else" +
                "      local splitArr = getPathAndThreadId(lock[i], -7)\n" +
                "      path = splitArr[2]\n " +
                "      threadId = splitArr[1]\n" +
                "      KEYS[i + 3 + total] = path..\"-\"..ARGV[2]\n" +
                "      result[length + 1] = redis.call('HGET', KEYS[i + 3 + total], ARGV[7])\n" +
                "      if result[length + 1] == false or result[length + 1] == nil\n" +
                "      then\n" +
                "        result[length + 1] = -1\n" +
                "      end\n" +
                "    end" +
                "    length = length + 2\n" +
                "end\n" +
                "for i = 1, #wait do\n" +
                "  result[length] = wait[i]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "result[1] = 1\n" +
                "result[2] = \"\"" +
                "return result";
    }

    @Override
    public String script() {
        return script;
    }
}
