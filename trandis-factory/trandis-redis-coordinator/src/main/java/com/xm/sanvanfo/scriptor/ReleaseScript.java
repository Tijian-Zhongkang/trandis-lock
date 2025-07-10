package com.xm.sanvanfo.scriptor;

public class ReleaseScript extends AbstractReleaseScript {

    private final static String script;
    static {
        String checkLeader = checkLeader("arr");
        script = releaseReadLock() +
                releaseWriteLock() +
                "local arr = {}\n" +
                "arr[1] = \"-1\"\n" +
                "arr[2] = \"leader is changed\"\n" +
                checkLeader +
                "local length = 6\n" +
                "arr[3] = 0\n" +
                "arr[4] = 0\n" +
                "arr[5] = 0\n" +
                "local keySize = #KEYS - 3;\n" +
                "local msg = \"\"\n" +
                "local releasePaths = {}\n" +
                "local releaseFailPaths = {}" +
                "local pathLen = 1\n" +
                "local failPathLen = 1\n" +
                "local lockPath\n" +
                "local reEnterTimes\n" +
                "for i = 4, keySize + 3 do\n" +
                "  reEnterTimes = ARGV[keySize  + i + 7] + 0\n" +
                "  if (ARGV[ 7 + i] == '1')\n" +
                "  then\n" +
                "    local writeArr = write_release(ARGV[3], KEYS[i], keySize * 3 + i, keySize * 4 + i , keySize * 5 + i, ARGV[keySize  + i + 7] + 0, ARGV[5], ARGV[6], ARGV[7], ARGV[9], ARGV[10])\n" +
                "    lockPath =  KEYS[i] .. \"-\"..ARGV[3]..\"-write\"" +
                "    if (writeArr[1] ~= 0)\n" +
                "    then\n" +
                "      releasePaths[pathLen] = lockPath\n" +
                "      releasePaths[pathLen + 1] = reEnterTimes\n" +
                "      pathLen = pathLen + 2\n" +
                "      for i = 2,#writeArr do\n" +
                "        arr[length] = writeArr[i]\n" +
                "        length = length + 1\n" +
                "      end\n" +
                "      if ARGV[keySize  + i + 7] + 0 <= 0\n" +
                "      then\n" +
                "        redis.call('SREM', KEYS[3], lockPath)\n" +
                "      end\n" +
                "    else\n" +
                "      msg = msg .. lockPath..\",\"\n" +
                "      redis.call('SREM', KEYS[2], lockPath)\n" +
                "      releaseFailPaths[failPathLen] = lockPath\n" +
                "      releaseFailPaths[failPathLen + 1] = reEnterTimes\n" +
                "      failPathLen = failPathLen + 2\n" +
                "    end\n" +
                "  else\n" +
                "    local readArr = read_release(ARGV[3], KEYS[i], keySize * 3 + i, keySize * 2 + i, keySize * 4 + i,  keySize * 5 + i, keySize * 6 + i, ARGV[keySize  + i + 7] + 0, ARGV[5], ARGV[4], ARGV[6], ARGV[7], ARGV[8], ARGV[9])\n" +
                "    lockPath =  KEYS[i] .. \"-\"..ARGV[3]..\"-read\""  +
                "    if (readArr[1] ~= 0)\n" +
                "    then\n" +
                "      releasePaths[pathLen] = lockPath\n" +
                "      releasePaths[pathLen + 1] = reEnterTimes\n" +
                "      pathLen = pathLen + 2\n" +
                "      for i = 2,#readArr do\n" +
                "        arr[length] = readArr[i]\n" +
                "        length = length + 1\n" +
                "      end\n" +
                "      if ARGV[keySize  + i + 7] + 0 <= 0\n" +
                "      then\n" +
                "        redis.call('SREM', KEYS[3], lockPath)\n" +
                "      end\n" +
                "    else\n" +
                "      msg = msg .. lockPath..\",\"\n" +
                "      redis.call('SREM', KEYS[2], lockPath)\n" +
                "      releaseFailPaths[failPathLen] = lockPath\n" +
                "      releaseFailPaths[failPathLen + 1] = reEnterTimes\n" +
                "      failPathLen = failPathLen + 2\n" +
                "    end\n" +
                "  end\n" +
                "end\n" +
                "if (string.len(msg) == 0)\n" +
                "then\n" +
                "  arr[1] = \"1\"\n" +
                "  arr[2] = \"\"\n" +
                "else\n" +
                "  arr[1] = \"0\"\n" +
                "  arr[2] = string.sub(msg, 1, string.len(msg) - 1)..\" release error no locked or locked by other\"\n" +
                "end\n" +
                "arr[3] = #arr - 5\n" +
                "arr[4] = #releasePaths / 2\n" +
                "arr[5] = #releaseFailPaths / 2\n" +
                "for i = 1, #releasePaths do\n" +
                "  arr[length] = releasePaths[i]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "for i = 1, #releaseFailPaths do\n" +
                "  arr[length] = releaseFailPaths[i]\n" +
                "  length = length + 1\n" +
                "end\n" +
                "return arr\n";
    }
    @Override
    public String script() {
        return script;
    }
}
