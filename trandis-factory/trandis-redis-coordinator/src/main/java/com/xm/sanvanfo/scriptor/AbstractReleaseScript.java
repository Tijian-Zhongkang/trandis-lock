package com.xm.sanvanfo.scriptor;

abstract class AbstractReleaseScript extends LockScript {

    static String releaseWriteLock() {
        return lastFlag() +
                "local function write_release(threadId, path, write, wait, notify, reEnter, writeSuffix, waitSuffix, notifySuffix, writeParamSuffix, id)\n" +
                "  KEYS[write] = path..\"-\"..writeSuffix\n" +
                "  KEYS[wait] =  path..\"-\"..waitSuffix\n" +
                "  KEYS[notify] = path..\"-\"..notifySuffix\n" +
                "  local arr = {}\n" +
                "  arr[1] = 1\n" +
                "  local length = 2\n" +
                "  local del = false\n" +
                "  local writeThread = redis.call('HGET', KEYS[write], id)\n" +
                "  if (writeThread == nil or writeThread == false or writeThread ~= threadId)\n" +
                "  then\n" +
                "    redis.call('LREM', KEYS[wait], 1, threadId..\"-write\")\n" +
                "    local delNotify = redis.call('SREM', KEYS[notify], threadId..\"-write\")\n" +
                "    if delNotify == 0\n" +
                "    then\n" +
                "      arr[1] = 0\n" +
                "      return arr\n" +
                "    else\n" +
                "      local notifySize = redis.call('SCARD', KEYS[notify])\n" +
                "      if notifySize > 0\n" +
                "      then\n" +
                "        arr[1] = 0\n" +
                "        return arr\n" +
                "      elseif (writeThread == nil or writeThread == false)\n" +
                "      then\n" +
                "        del = true\n" +
                "      else\n" +
                "        arr[1] = 0\n" +
                "        return arr\n" +
                "      end\n" +
                "    end\n" +
                "  end\n" +
                "  if del == false\n" +
                "  then\n" +
                "    local reEn = redis.call('HGET', KEYS[write], writeParamSuffix)\n" +
                "    if (reEn == nil or reEn == false or reEn + 0 == -1)\n" +
                "    then\n" +
                "      del = true\n" +
                "    elseif reEn + 0 > reEnter\n" +
                "    then\n" +
                "      if reEnter > 0\n" +
                "      then\n" +
                "        redis.call('HSET', KEYS[write], writeParamSuffix, reEnter)\n" +
                "      else\n" +
                "        del = true\n" +
                "      end\n" +
                "    end\n" +
                "  end\n" +
                "  if del == true\n" +
                "  then\n" +
                "    redis.call('DEL', KEYS[write])\n" +
                "    local item = redis.call('RPOP', KEYS[wait])\n" +
                "    while (item ~= nil and item ~= false)\n" +
                "    do\n" +
                "      local flag = lastFlag(item, \"-\")\n" +
                "      if flag ~= \"read\"\n" +
                "      then\n" +
                "        if (#arr == 1)\n" +
                "        then\n" +
                "          arr[length] = path..\"-\"..item\n" +
                "          redis.call('SADD', KEYS[notify], item)\n" +
                "          length = length + 1\n" +
                "        else\n" +
                "          redis.call('RPUSH', KEYS[wait], item)\n" +
                "        end\n" +
                "        break\n" +
                "      end\n" +
                "      arr[length] = path..\"-\"..item\n" +
                "      redis.call('SADD', KEYS[notify], item)\n" +
                "      length = length + 1\n" +
                "      item = redis.call('RPOP', KEYS[wait])\n" +
                "    end\n" +
                "    return arr\n" +
                "  end\n" +
                "  return arr\n" +
                "end\n";
    }

    static String releaseReadLock() {
        return  "local function read_release(threadId, path, write, read, wait, notify, param, reEnter, writeSuffix, readSuffix, waitSuffix, notifySuffix, readParamSuffix, writeParamSuffix)\n" +
                "  KEYS[write] = path..\"-\"..writeSuffix\n" +
                "  KEYS[read] = path..\"-\"..readSuffix\n" +
                "  KEYS[wait] = path..\"-\"..waitSuffix\n" +
                "  KEYS[notify] = path..\"-\"..notifySuffix\n" +
                "  KEYS[param] = path..\"-\"..readParamSuffix\n" +
                "  local arr = {}\n" +
                "  arr[1] = 1\n" +
                "  local length = 2\n" +
                "  local del = false\n" +
                "  local hasLock = redis.call('SISMEMBER', KEYS[read], threadId)\n" +
                "  if (hasLock == 0)\n" +
                "  then\n" +
                "    redis.call('LREM', KEYS[wait], 1, threadId..\"-read\")\n" +
                "    local delNotify = redis.call('SREM', KEYS[notify], threadId..\"-read\")\n" +
                "    if delNotify == 0\n" +
                "    then\n" +
                "      arr[1] = 0\n" +
                "      return arr\n" +
                "    else\n" +
                "      local notifySize = redis.call('SCARD', KEYS[notify])\n" +
                "      if notifySize > 0\n" +
                "      then\n" +
                "        arr[1] = 0\n" +
                "        return arr\n" +
                "      else\n" +
                "        del = true\n" +
                "      end\n"+
                "    end\n" +
                "  end\n" +
                "  if del == false\n" +
                "  then\n" +
                "    local reEn = redis.call('HGET', KEYS[param], threadId)\n" +
                "    if (reEn == nil or reEn == false or reEn + 0 == -1)\n" +
                "    then\n" +
                "      del = true\n" +
                "    elseif (reEn + 0 > reEnter)\n" +
                "    then\n" +
                "      if (reEnter > 0)\n" +
                "      then\n" +
                "        redis.call('HSET', KEYS[param], threadId, reEnter)\n" +
                "      else\n" +
                "        del = true\n" +
                "      end\n" +
                "    else\n" +
                "      return arr\n" +
                "    end\n" +
                "  end\n" +
                "  if (del == true)\n" +
                "  then\n" +
                "    redis.call('HDEL', KEYS[param], threadId)\n" +
                "    redis.call('SREM', KEYS[read], threadId)\n" +
                "    --check if none read and notify other thread\n" +
                "    local size = redis.call('SCARD', KEYS[read])\n" +
                "    local writeThread = redis.call('HGET', KEYS[write], writeParamSuffix)\n" +
                "    if size == 0 and (writeThread == false or writeThread == nil)\n" +
                "    then\n" +
                "      local item = redis.call('RPOP', KEYS[wait])\n" +
                "      if (item ~= nil and item ~= false)\n" +
                "      then\n" +
                "        arr[length] = path..\"-\"..item\n" +
                "        redis.call('SADD', KEYS[notify], item)\n" +
                "        length = length + 1\n" +
                "      end\n" +
                "    end\n" +
                "    return arr\n" +
                "  end\n" +
                "  return arr\n" +
                "end\n";
    }


}
