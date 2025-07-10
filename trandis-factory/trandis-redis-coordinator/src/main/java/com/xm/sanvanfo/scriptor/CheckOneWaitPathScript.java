package com.xm.sanvanfo.scriptor;

public class CheckOneWaitPathScript extends LockScript {

    private final static String script;
    static {
        script = lastFlag() +
                "local arr={}\n" +
                "arr[1] = -1\n" +
                "arr[2] = \"leader is changed\"\n" +
                checkLeader("arr") +
                "local i\n" +
                "KEYS[3] = KEYS[2]..\"-\"..ARGV[4]\n" +
                "KEYS[4] = KEYS[2]..\"-\"..ARGV[5]\n" +
                "local waitLen = redis.call('LLEN', KEYS[3])\n" +
                "if waitLen > 0\n" +
                "then\n" +
                "  arr[1] = 0\n" +
                "  arr[2] = \"\"\n" +
                "  local waitArr = redis.call('LRANGE', KEYS[3],  0, waitLen - 1)\n" +
                "  for i = 1, #waitArr do\n" +
                "    if waitArr[i] == ARGV[3]\n" +
                "    then\n" +
                "      arr[1] = 1\n" +
                "      break\n" +
                "    end\n" +
                "  end\n" +
                "  if ARGV[6] == \"check\"\n" +
                "  then\n" +
                "    arr[2] = ARGV[3]..\" checking is \"..arr[1]\n" +
                "    if arr[1] == 0\n" +
                "    then\n" +
                "      if redis.call('SISMEMBER', KEYS[4], ARGV[3]) == 1\n" +
                "      then\n" +
                "        arr[1] = 2\n" +
                "      end\n" +
                "    end\n" +
                "    return arr\n" +
                "  end\n" +
                "  if arr[1] ~= 1\n" +
                "  then\n" +
                "    redis.call('SREM', KEYS[4], ARGV[3])\n" +
                "    local notifySize = redis.call('SCARD', KEYS[4])\n" +
                "    if notifySize  > 0\n" +
                "    then\n" +
                "      return arr\n" +
                "    end\n" +
                "    arr[2] = ARGV[3]..\" is not in waiting, right pop item\"\n" +
                "    local item = redis.call('RPOP', KEYS[3])\n" +
                "    local length = 3\n" +
                "    while item ~= nil and item ~= false\n" +
                "    do\n" +
                "      local flag = lastFlag(item, \"-\")\n" +
                "      if flag ~= \"read\"\n" +
                "      then\n" +
                "        if (#arr == 2)\n" +
                "        then\n" +
                "          arr[length] = KEYS[2]..\"-\"..item\n" +
                "          redis.call('SADD', KEYS[4], item)\n" +
                "          length = length + 1\n" +
                "        else\n" +
                "          redis.call('RPUSH', KEYS[3], item)\n" +
                "        end\n" +
                "        break\n" +
                "      end\n" +
                "      arr[length] = KEYS[2]..\"-\"..item\n" +
                "      redis.call('SADD', KEYS[4], item)\n" +
                "      length = length + 1\n" +
                "      item = redis.call('RPOP', KEYS[3])\n" +
                "    end\n" +
                "  end\n" +
                "else\n" +
                "  arr[1] = 0\n" +
                "  arr[2] = \"\"\n" +
                "  if ARGV[6] == \"check\"\n" +
                "  then\n" +
                "    arr[2] = ARGV[3]..\" checking is \"..arr[1]\n" +
                "  end\n" +
                "end\n" +
                "return arr";
    }
    @Override
    public String script() {
        return  script;
    }
}
