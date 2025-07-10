package com.xm.sanvanfo.scriptor;

public class DeleteFollowerScript extends AbstractReleaseScript {

    private final static String script;

    static {
        String checkLeader = checkLeader("result");
        script =  releaseReadLock() +
                releaseWriteLock() +
                lastFlag() +
                getPathAndThreadId() +
                deleteSetKey() +
                "local result = {}\n" +
                "result[1] = \"-1\"" +
                "result[2] = \"leader is changed\"" +
                checkLeader +
                "local wait = redis.call('SMEMBERS', KEYS[2])\n" +
                "local lock = redis.call('SMEMBERS', KEYS[3])\n" +
                "local arr = {}\n" +
                "result[3] = 0\n" +
                "result[4] = #wait\n" +
                "result[5] = #lock\n" +
                "local i = 1\n" +
                "local j\n" +
                "for j=1, #wait do\n" +
                "  arr[i] = wait[j]\n" +
                "  i = i + 1\n" +
                "end\n" +
                "for j=1, #lock do\n" +
                "  arr[i] = lock[j]\n" +
                "  i = i + 1\n" +
                "end\n" +
                "deleteSetKey(arr, result, 6)\n" +
                "result[3] = #result - 5\n" +
                "local length = #result + 1\n" +
                "for j= 1,#arr do\n" +
                "  result[length] = arr[j]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "redis.call('DEL', KEYS[2])\n" +
                "redis.call('DEL', KEYS[3])\n" +
                "return result";
    }

    @Override
    public String script() {
        return script;
    }

    //release lock return 0  is also true because of lock waiting and we can do nothing for error
    private static String deleteSetKey() {
        return  "local function deleteSetKey(arr, ret, index)\n" +
                "  ret[1] = \"1\"\n" +
                "  ret[2] = \"\"\n" +
                "  local totalSize = #KEYS\n" +
                "  local length = index\n" +
                "    for i=1,#arr do\n" +
                "      local flag = lastFlag(arr[i], \"-\")\n" +
                "      local path\n" +
                "      local threadId\n" +
                "      if flag == \"read\"\n" +
                "      then\n" +
                "        local splitArr = getPathAndThreadId(arr[i], -6)\n" +
                "        path = splitArr[2]\n " +
                "        threadId = splitArr[1]\n" +
                "        local readRet = read_release(threadId, path, totalSize + i, totalSize * 2  + i, totalSize * 3  + i, totalSize * 4  + i, totalSize * 5  + i, 0, ARGV[4], ARGV[3], ARGV[5], ARGV[6], ARGV[7], ARGV[8])\n" +
                "        if readRet[1] ~= 0\n" +
                "        then\n" +
                "          for i = 2, #readRet do\n" +
                "            ret[length] = readRet[i]\n" +
                "            length = length + 1\n" +
                "          end\n" +
                "        else\n" +
                "          ret[2] = ret[2]..path..\"-\"..threadId..\"-read,\"\n" +
                "        end\n" +
                "      else\n" +
                "        local splitArr = getPathAndThreadId(arr[i], -7)\n" +
                "        path = splitArr[2]\n " +
                "        threadId = splitArr[1]\n" +
                "        local writeRet = write_release(threadId, path, totalSize + i, totalSize * 3 + i, totalSize * 4  + i, 0, ARGV[4], ARGV[5], ARGV[6], ARGV[8], ARGV[9])\n" +
                "        if writeRet[1] ~= 0\n" +
                "        then\n" +
                "          for i = 2, #writeRet do\n" +
                "            ret[length] = writeRet[i]\n" +
                "            length = length + 1\n" +
                "          end\n" +
                "        else\n" +
                "          ret[2] = ret[2]..path..\"-\"..threadId..\"-write,\"\n" +
                "        end\n" +
                "      end\n" +
                "    end\n" +
                "    if (string.len(ret[2]) > 0)\n" +
                "    then\n" +
                "      ret[2] = string.sub(ret[2], 1, string.len(ret[2]) - 1)..\" is waiting for locking\"\n" +
                "    end\n" +
                "  end\n";
    }
}
