package com.xm.sanvanfo.scriptor;

abstract class LockScript implements IScript {


    static String checkLeader(String ret) {

       return String.format("local leaderId = redis.call('HGET', KEYS[1], ARGV[1])\n" +
                "if (leaderId == nil or leaderId == false or leaderId ~= ARGV[2])\n" +
                "then\n" +
                "  return %s\n" +
                "end\n", ret);

    }

    static String lastFlag() {
        return  "local function lastFlag(str, split)\n" +
                "  local reverse = string.reverse(str)\n" +
                "  local find = string.find(reverse, split)\n" +
                "  if find == nil\n" +
                "  then\n" +
                "    return nil\n" +
                "  else\n" +
                "    local flag = string.sub(reverse, 1, find - 1)\n" +
                "    return string.reverse(flag)\n" +
                "  end\n" +
                "end\n";
    }

    static String getPathAndThreadId() {
        return "local function getPathAndThreadId(str, index)\n" +
                "  local arr = {}\n" +
                "  local path\n" +
                "  local threadId\n" +
                "  local coordinatorIdUuid\n" +
                "  local coordinatorIdIp\n" +
                "  path = string.sub(str, 1, index)\n" +
                "  threadId = lastFlag(path, \"-\")\n" +
                "  path = string.sub(path, 1, -string.len(threadId) - 2)\n" +
                "  coordinatorIdUuid = lastFlag(path, \"-\")\n" +
                "  path = string.sub(path, 1, -string.len(coordinatorIdUuid) - 2)\n" +
                "  if path ~= nil\n" +
                "  then\n" +
                "    coordinatorIdIp = lastFlag(path, \"-\")\n" +
                "    if coordinatorIdIp == nil\n" +
                "    then\n" +
                "      coordinatorIdIp = path\n" +
                "      path = nil\n" +
                "    else\n" +
                "      path = string.sub(path, 1, -string.len(coordinatorIdIp) - 2)\n" +
                "    end\n" +
                "  end\n" +
                "  arr[1] = coordinatorIdIp..\"-\"..coordinatorIdUuid..\"-\"..threadId\n" +
                "  arr[2] = path\n" +
                "  arr[3] = coordinatorIdIp..\"-\"..coordinatorIdUuid\n" +
                "  return arr\n" +
                "end\n";
    }
}
