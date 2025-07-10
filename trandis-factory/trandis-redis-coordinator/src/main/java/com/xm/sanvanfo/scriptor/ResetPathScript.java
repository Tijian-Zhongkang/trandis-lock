package com.xm.sanvanfo.scriptor;

public class ResetPathScript extends LockScript {

    private final static String script;
    static {
        String checkLeader = checkLeader("result");
        script = lastFlag() +
                getPathAndThreadId() +
                "local result={}\n" +
                "result[1] = -1\n" +
                "result[2] = \"leader is changed\"\n" +
                checkLeader +
                "local table = {}\n" +
                "KEYS[5] = KEYS[2]..\"-\"..ARGV[3]\n" +            //readSuffix
                "KEYS[6] = KEYS[2]..\"-\"..ARGV[4]\n" +            //writeSuffix
                "KEYS[7] = KEYS[2]..\"-\"..ARGV[5]\n" +            //waitSuffix
                "KEYS[8] = KEYS[2]..\"-\"..ARGV[6]\n" +           //notifySuffix
                "KEYS[9] = KEYS[2]..\"-\"..ARGV[7]\n" +           //readParamSuffix
                "local writeLock = redis.call('HGET', KEYS[6], ARGV[9])\n" +
                "if writeLock ~= nil and writeLock ~= false\n" +
                "then\n" +
                "  local parts = getPathAndThreadId(writeLock, -1)\n" +
                "  table[parts[3]..\"-\"..KEYS[4]] = {}\n" +
                "end\n" +
                "local readLock = redis.call('SMEMBERS', KEYS[5])\n" +
                "if readLock ~= nil\n" +
                "then\n" +
                "  for i = 1,#readLock do\n" +
                "    local parts = getPathAndThreadId(readLock[i], -1)\n" +
                "    table[parts[3]..\"-\"..KEYS[4]] = {}\n" +
                "  end\n" +
                "end\n" +
                "local wait = redis.call('LRANGE', KEYS[7], 0, -1)\n" +
                "if wait ~= nil\n" +
                "then\n" +
                "  for i = 1,#wait do\n" +
                "    local flag = lastFlag(wait[i], \"-\")\n" +
                "    local parts = getPathAndThreadId(wait[i], -2 - string.len(flag))\n" +
                "    table[parts[3]..\"-\"..KEYS[3]] = {}\n" +
                "  end\n" +
                "end\n" +
                "local length = 14\n" +
                "redis.call('DEL',KEYS[6])\n" +
                "if ARGV[10] + 0 == 1\n" +
                "then\n" +
                "  local flag = lastFlag(ARGV[length], \"-\")\n" +
                "  local parts = getPathAndThreadId(ARGV[length], -2 - string.len(flag))\n" +
                "  redis.call('HMSET', KEYS[6], ARGV[9], parts[1], ARGV[8], ARGV[length + 1])\n" +
                "  if table[parts[3]..\"-\"..KEYS[4]] == nil\n" +
                "  then\n" +
                "    table[parts[3]..\"-\"..KEYS[4]] = {}\n" +
                "  end\n" +
                "  table[parts[3]..\"-\"..KEYS[4]][#table[parts[3]..\"-\"..KEYS[4]] + 1] = KEYS[2]..\"-\"..ARGV[length]\n" +
                "  length = length + 2\n" +
                "end\n" +
                "redis.call('DEL', KEYS[5])\n" +
                "redis.call('DEL', KEYS[9])\n" +
                "for i = 1, ARGV[11] + 0 do\n" +
                "  local flag = lastFlag(ARGV[length], \"-\")\n" +
                "  local parts = getPathAndThreadId(ARGV[length], -2 - string.len(flag))\n" +
                "  redis.call('SADD', KEYS[5], parts[1])\n" +
                "  redis.call('HSET', KEYS[9], parts[1], ARGV[length + 1] + 0)\n" +
                "  if table[parts[3]..\"-\"..KEYS[4]] == nil\n" +
                "  then\n" +
                "    table[parts[3]..\"-\"..KEYS[4]] = {}\n" +
                "  end\n" +
                "  table[parts[3]..\"-\"..KEYS[4]][#table[parts[3]..\"-\"..KEYS[4]] + 1] = KEYS[2]..\"-\"..ARGV[length]\n" +
                "  length = length + 2\n" +
                "end\n" +
                "redis.call('DEL', KEYS[7])\n" +
                "for i = 1, ARGV[12] do\n" +
                "  local flag = lastFlag(ARGV[length], \"-\")\n" +
                "  local parts = getPathAndThreadId(ARGV[length], -2 - string.len(flag))\n" +
                "  redis.call('LPUSH', KEYS[7], ARGV[length])\n" +
                "  if table[parts[3]..\"-\"..KEYS[3]] == nil\n" +
                "  then\n" +
                "    table[parts[3]..\"-\"..KEYS[3]] = {}\n" +
                "  end\n" +
                "  table[parts[3]..\"-\"..KEYS[3]][#table[parts[3]..\"-\"..KEYS[3]] + 1] = KEYS[2]..\"-\"..ARGV[length]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "redis.call('DEL', KEYS[8])\n" +
                "for i = 1, ARGV[13] do\n" +
                "  local flag = lastFlag(ARGV[length], \"-\")\n" +
                "  local parts = getPathAndThreadId(ARGV[length], -2 - string.len(flag))\n" +
                "  redis.call('SADD', KEYS[8], ARGV[length])" +
                "  if table[parts[3]..\"-\"..KEYS[3]] == nil\n" +
                "  then\n" +
                "    table[parts[3]..\"-\"..KEYS[3]] = {}\n" +
                "  end\n" +
                "  table[parts[3]..\"-\"..KEYS[3]][#table[parts[3]..\"-\"..KEYS[3]] + 1] = KEYS[2]..\"-\"..ARGV[length]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "local keyLen = 10\n" +
                "result[2] = \"\"\n" +
                "for key, value in pairs(table) do\n" +
                "  if value ~= false and value ~= nil\n" +
                "  then\n" +
                "    KEYS[keyLen] = key\n " +
                "    result[2] = result[2]..key..\",\"\n" +
                "    local members = redis.call('SMEMBERS', KEYS[keyLen])\n" +
                "    for i = 1, #members do\n" +
                "      local flag = lastFlag(members[i], \"-\")\n" +
                "      local parts = getPathAndThreadId(members[i],  -2 - string.len(flag))\n" +
                "      if parts[2] == KEYS[2]\n" +
                "      then\n" +
                "        redis.call('SREM', KEYS[keyLen], members[i])\n" +
                "      end\n" +
                "    end\n" +
                "    for j = 1, #value do\n" +
                "      redis.call('SADD',  KEYS[keyLen], value[j])\n" +
                "    end\n" +
                "    keyLen = keyLen + 1\n" +
                "  end\n" +
                "end\n" +
                "result[1] = 1\n" +
                "return result\n";

    }

    @Override
    public String script() {
        return script;
    }
}
