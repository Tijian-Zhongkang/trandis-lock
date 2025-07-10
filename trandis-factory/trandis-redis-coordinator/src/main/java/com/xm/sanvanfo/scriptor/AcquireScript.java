package com.xm.sanvanfo.scriptor;

public class AcquireScript extends LockScript {

    private final static String script;

    static {
        String checkLeader = checkLeader("result");
        script = "local function exists_wait(key, path)\n" +
                "  local waits = redis.call('LRANGE', key, 0, -1)\n" +
                "  for i = 1, #waits do\n" +
                "    if waits[i] == path\n" +
                "    then\n" +
                "      return 1\n" +
                "    end\n" +
                "  end\n" +
                "  return 0\n" +
                "end\n" +
                "local function read_lock(threadId, path, reEnter, i , size, readSuffix, writeSuffix, waitSuffix, notifySuffix, readParamSuffix, id)\n" +
                "  KEYS[2 *size + i] = path..\"-\"..readSuffix\n" +
                "  KEYS[3 * size + i] = path..\"-\"..writeSuffix\n"+
                "  KEYS[4 * size + i] = path..\"-\"..waitSuffix\n" +
                "  KEYS[5 * size + i] = path..\"-\"..notifySuffix\n" +
                "  KEYS[6 * size + i] = path..\"-\"..readParamSuffix\n" +
                "  local addWait = false\n" +
                "  local writeThread = redis.call('HGET', KEYS[3 * size + i], id)\n" +
                "  local waitReady =  exists_wait(KEYS[4 * size + i], threadId..\"-read\")\n" +
                "  if waitReady == 1\n" +
                "  then\n" +
                "    if writeThread ~= threadId\n" +
                "    then\n" +
                "      return false\n" +
                "    else\n" +
                "      redis.call('LREM', KEYS[4 * size + i], threadId..\"-read\")\n" +
                "    end\n" +
                "  end\n" +
                "  local isLocked = redis.call('SISMEMBER', KEYS[2 * size + i], threadId)\n" +
                "  if isLocked == 1\n" +
                "  then\n" +
                "    addWait = false\n" +
                "  else\n" +
                "    local notify = redis.call('SCARD', KEYS[5 * size + i])\n" +
                "    if notify > 0 and writeThread ~= threadId\n" +
                "    then\n" +
                "      local isNotify = redis.call('SISMEMBER', KEYS[5 * size + i], threadId..\"-read\")\n" +
                "      if isNotify == 0\n" +
                "      then\n" +
                "        addWait = true\n" +
                "      end\n" +
                "    else\n" +
                "      local hasWait = redis.call('LLEN', KEYS[4 * size + i])\n" +
                "      if hasWait > 0 and writeThread ~= threadId\n" +
                "      then\n" +
                "        addWait = true\n" +
                "      end\n" +
                "    end\n" +
                "  end\n" +
                "  if addWait == false\n" +
                "  then\n" +
                "    redis.call('SREM', KEYS[5 * size + i], threadId..\"-read\")\n" +
                "    if (writeThread == nil or writeThread == false or writeThread == threadId)\n" +
                "    then\n" +
                "      redis.call('SADD', KEYS[2 * size + i], threadId)" +
                "      if (reEnter ~= -1)\n" +
                "      then\n" +
                "        local reEn = redis.call('HGET', KEYS[6 * size + i], threadId)\n" +
                "        if (reEn == nil or reEn == false or (reEn ~= nil and reEn + 0 < reEnter))\n" +
                "        then\n" +
                "          redis.call('HSET',  KEYS[6 * size + i], threadId, reEnter)\n" +
                "        end\n" +
                "      end\n" +
                "      return true\n" +
                "    else\n" +
                "      addWait = true\n" +
                "    end\n" +
                "  end\n" +
                "  if addWait == true\n" +
                "  then\n" +
                "    if waitReady == 0\n" +
                "    then\n" +
                "      redis.call('LPUSH',  KEYS[4 * size + i], threadId..\"-read\")\n" +
                "      return false\n" +
                "    end\n" +
                "  end\n" +
                "end\n" +
                "local function write_lock(threadId, path, reEnter, i , size, readSuffix, writeSuffix, waitSuffix, notifySuffix, writeParamSuffix, id)\n" +
                "  KEYS[2 * size + i] = path..\"-\"..readSuffix\n" +
                "  KEYS[3 * size + i] = path..\"-\"..writeSuffix\n" +
                "  KEYS[4 * size + i] = path..\"-\"..waitSuffix\n" +
                "  KEYS[5 * size + i] = path..\"-\"..notifySuffix\n" +
                "  local addWait = false\n" +
                "  local waitReady = exists_wait(KEYS[4 * size + i], threadId..\"-write\")\n" +
                "  if waitReady == 1\n" +
                "  then\n" +
                "    return false\n" +
                "  end\n" +
                "  local writeThread = redis.call('HGET', KEYS[3 * size + i], id)\n" +
                "  if (writeThread == threadId)\n" +
                "  then\n" +
                "    addWait = false\n" +
                "  else\n" +
                "    local notify = redis.call('SCARD', KEYS[5 * size + i])\n" +
                "    if notify > 0\n" +
                "    then\n" +
                "      local isNotify = redis.call('SISMEMBER', KEYS[5 * size + i], threadId..\"-write\")\n" +
                "      if isNotify == 0\n" +
                "      then\n" +
                "        addWait = true\n" +
                "      end\n" +
                "    end\n" +
                "  end\n" +
                "  if addWait == false\n" +
                "  then\n" +
                "    redis.call('SREM', KEYS[5 * size + i], threadId..\"-write\")\n" +
                "    if (writeThread == threadId)\n" +
                "    then\n" +
                "      if (reEnter ~= -1)\n" +
                "      then\n" +
                "        local reEn = redis.call('HGET', KEYS[3 * size + i], writeParamSuffix) + 0\n" +
                "        if (reEn == nil or reEn == false or (reEn ~= nil and reEn < reEnter))\n" +
                "        then\n" +
                "          redis.call('HSET', KEYS[3 * size + i], writeParamSuffix, reEnter)" +
                "        end\n" +
                "      end\n" +
                "      return true\n" +
                "    elseif (writeThread == nil or writeThread == false)\n" +
                "    then\n" +
                "      local len = redis.call('SCARD', KEYS[2 * size + i])\n" +
                "      if (len > 0)\n" +
                "      then\n" +
                "        addWait = true\n" +
                "      else\n" +
                "        redis.call('HSET', KEYS[3 * size + i], id, threadId)\n" +
                "        if (reEnter ~= -1)\n" +
                "        then\n" +
                "          local reEn = redis.call('HGET', KEYS[3 * size + i], writeParamSuffix)\n" +
                "          if (reEn == nil or reEn == false or (reEn ~= nil and reEn + 0 < reEnter))\n" +
                "          then\n" +
                "            redis.call('HSET', KEYS[3 * size + i], writeParamSuffix, reEnter)" +
                "          end\n" +
                "        end\n" +
                "        return true" +
                "      end\n" +
                "    else\n" +
                "      addWait = true\n" +
                "    end\n" +
                "  end\n" +
                "  if addWait == true\n" +
                "  then\n" +
                "    if waitReady == 0\n" +
                "    then\n" +
                "      redis.call('LPUSH',  KEYS[4 * size + i], threadId..\"-write\")\n" +
                "    end\n" +
                "    return false\n" +
                "  end\n" +
                "end\n" +
                "local result = {}\n" +
                "result[1] = \"-1\"\n" +
                "result[2] = \"leader is changed\"\n" +
                "result[3] = \"\"\n" +
                "local t = 4\n" +
                "local keySize = #KEYS - 3;\n"+
                checkLeader +
                "local ret\n" +
                "local lockPath\n" +
                "local reEnterTimes\n" +
                "for i=4, keySize + 3 do \n" +
                "  reEnterTimes = ARGV[keySize + i + 7] + 0\n" +
                "  if (ARGV[i + 7] == '1')\n" +
                "  then\n" +
                "    ret = write_lock(ARGV[3], KEYS[i], ARGV[keySize + i + 7] + 0, i, keySize, ARGV[4], ARGV[5], ARGV[6], ARGV[7], ARGV[9], ARGV[10])\n" +
                "    lockPath =  KEYS[i] ..\"-\"..ARGV[3]..\"-write\"\n" +
                "  else\n" +
                "    ret = read_lock(ARGV[3], KEYS[i], ARGV[keySize + i + 7] + 0, i, keySize, ARGV[4], ARGV[5], ARGV[6], ARGV[7], ARGV[8], ARGV[10])\n" +
                "    lockPath =  KEYS[i]..\"-\"..ARGV[3]..\"-read\"\n" +
                "  end\n" +
                "  if (ret == false)\n" +
                "  then\n" +
                "    redis.call('SADD', KEYS[2], lockPath)\n" +
                "    result[1] = \"0\"\n" +
                "    result[2] = lockPath ..\" is waiting\"\n" +
                "    result[3] = lockPath\n" +
                "    return result\n" +
                "  else\n" +
                "    redis.call('SREM', KEYS[2],lockPath)\n" +
                "    redis.call('SADD', KEYS[3], lockPath)\n" +
                "    result[t] = lockPath\n" +
                "    result[t + 1] = reEnterTimes\n" +
                "    t = t + 2\n" +
                "  end\n" +
                "end\n" +
                "result[1] = \"1\"\n" +
                "result[2] = \"\"\n" +
                "return result";
    }

    @Override
    public String script() {
      return script;
    }
}
