import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xm.sanvanfo.BootstrapServerInfo;
import com.xm.sanvanfo.CoordinatorConfig;
import com.xm.sanvanfo.LettuceRedisLocker;
import com.xm.sanvanfo.LockerConvertFuture;
import com.xm.sanvanfo.scriptor.*;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LettuceRedisRedisLockerTest {

    @Test
    public void scriptStringTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        String lua = "return KEYS[1] .. \"2222334567\"";
        long begin = System.currentTimeMillis();
        for(int i = 0; i < 1000000; i++) {
            String result = lettuceRedisLocker.execScript(lua, String.class, null, Collections.singletonList("Test"), Collections.singletonList("cc"));
            //System.out.println(result);
        }
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptStringTest:" + (end - begin) + "ms");
    }

    @Test
    public void scriptStringSingleThreadAsyncTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        String lua = "return KEYS[1] .. \"22223345678901211996\"";
        List<LockerConvertFuture<String>> list = new ArrayList<>();
        long begin = System.currentTimeMillis();
        for(int i = 0; i < 1000000; i++) {
            LockerConvertFuture<String> future = lettuceRedisLocker.execScriptAsync(lua, String.class, null, Collections.singletonList("Test"), Collections.singletonList("cc"));
            list.add(future);
        }
        for (LockerConvertFuture<String> future:list
        ) {
            String result = future.get();
            //System.out.println(result);
        }
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptStringSingleThreadAsyncTest:" + (end - begin) + "ms");
    }


    @Test
    public void scriptStringAsyncTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]>[] lockers = new LettuceRedisLocker[20];
        for(int i = 0; i < 20; i++) {
            lockers[i] = newLettuceRedisLocker(defaultCoordinatorConfig());
        }
        //LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        String lua = "return KEYS[1] .. \"22223345678901211996\"";
        ExecutorService service = Executors.newFixedThreadPool(500);
        CountDownLatch latch = new CountDownLatch(1000000);
        List<LockerConvertFuture<String>> list = Collections.synchronizedList(new ArrayList<>());
        long begin = System.currentTimeMillis();
        for(int i = 0; i < 1000000; i++) {
            int count = i % 20;
            service.execute(()->{
                try {
                    LockerConvertFuture<String> future = lockers[count].execScriptAsync(lua, String.class, null, Collections.singletonList("Test"), Collections.singletonList("cc"));
                    list.add(future);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        for (LockerConvertFuture<String> future:list
             ) {
            String result = future.get();
            //System.out.println(result);
        }
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptStringAsyncTest:" + (end - begin) + "ms");
    }

    @Test
    public void scriptStringAsyncListenerTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        String lua = "return KEYS[1] .. \"2222334567899183666\"";
        for(int i = 0; i < 2; i++) {
            long begin = System.currentTimeMillis();
            LockerConvertFuture<String> future = lettuceRedisLocker.execScriptAsync(lua, String.class, null,
                    Collections.singletonList("Test"), Collections.singletonList("cc"));
            future.addConsumerListener((result, ex)->{
                long end = System.currentTimeMillis();
                System.out.println("--------------scriptStringAsyncListenerTest:" + (end - begin) + "ms");
                System.out.println(result);
            });
        }
    }

    @Test
    public void scriptTableTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        String lua = "return redis.call('HGETALL', KEYS[1])";
        List result = lettuceRedisLocker.execScript(lua, List.class, null, Collections.singletonList("Test"), Collections.emptyList());
        System.out.println(result);
    }

    @Test
    public void scriptsetNxMastInfoScriptTest() throws Exception {

        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        SetNxMastInfoScript setNxMastInfoScript = new SetNxMastInfoScript();
        Long master = lettuceRedisLocker.execScript(setNxMastInfoScript.script(), Long.class, null,  setNxMastInfoScript.scriptKey("Leader", "Election"),
                setNxMastInfoScript.scriptArgv("content", "{}", "id",
                        "abc", "activeTime", "2022-12-31", "2000000"));
        System.out.println(master);
    }

    @Test
    public void scriptReElectionTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ReElectionScript reElectionScript = new ReElectionScript();
        Long result = lettuceRedisLocker.execScript(reElectionScript.script(), Long.class, null, reElectionScript.scriptKey("Election",
                "coordinatorNodeList", "Leader"),
                reElectionScript.scriptArgv("aaaaaaa", 200000L, "id", "masteraaaaaab",  "activeTime", 123456789L));
        System.out.println(result);
    }

    @Test
    public void scriptCheckLeaderTest() throws Exception {
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        now = now - 20;
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        CheckLeaderScript script = new CheckLeaderScript();
        BootstrapServerInfo serverInfo = lettuceRedisLocker.execScript(script.script(), BootstrapServerInfo.class, null,
                script.scriptKey("coordinatorLeader"), script.scriptArgv("content", "activeTime", now));
        System.out.println(serverInfo);
    }

    @Test
    public void scriptAcquireSingLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        AcquireScript acquireScript = new AcquireScript();
        List result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path1"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId2", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));
    }

    @Test
    public void scriptAcquireMultiLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        AcquireScript acquireScript = new AcquireScript();
        long begin = System.currentTimeMillis();
        List result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path2", "Path3", "Path4", "Path5", "Path2", "Path12", "Path13", "Path14", "Path15", "Path12"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 0, 1, 0, 1,1, 0, 1, 0, 1, 1, -1, -1, -1, 2,1, -1, -1, -1, 2));
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptAcquireMultiLockTest:" + (end - begin) + "ms");
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));

    }

    @Test
    public void scriptAcquireReadWriteLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        AcquireScript acquireScript = new AcquireScript();
        List result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path6"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));

        result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path6"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 0, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        size = result.size();
        list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));
    }

    @Test
    public void scriptReEntrantLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        AcquireScript acquireScript = new AcquireScript();
        List result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path7"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));

        result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path7"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 2));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        size = result.size();
        list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));

        result = lettuceRedisLocker.execScript(acquireScript.script(), List.class, null,
                acquireScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path7"),
                acquireScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 3));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        size = result.size();
        list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("locks are:" + String.join(",", list));
    }

    @Test
    public void scriptReleaseSingleLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ReleaseScript releaseScript = new ReleaseScript();
        List result = lettuceRedisLocker.execScript(releaseScript.script(), List.class, null,
                releaseScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path1"),
                releaseScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId3", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 0, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));
    }

    @Test
    public void scriptReleaseMultiLockTest() throws Exception{
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ReleaseScript releaseScript = new ReleaseScript();
        long begin = System.currentTimeMillis();
        List result = lettuceRedisLocker.execScript(releaseScript.script(), List.class, null,
                releaseScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path2", "Path3", "Path4", "Path5", "Path2", "Path12", "Path13", "Path14", "Path15", "Path12"),
                releaseScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 0, 1, 0, 1,1, 0, 1, 0, 1, 1, -1, -1, -1, 0, 1, -1, -1, -1, 0));
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptReleaseMultiLockTest:" + (end - begin) + "ms");
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));
    }

    @Test
    public void ScriptTestMultiTest()  throws Exception{
        long begin = 0;
        for (int i = 0; i < 200; i++) {

            if( i == 1) {
                begin = System.currentTimeMillis();
            }
            scriptAcquireMultiLockTest();
            scriptReleaseMultiLockTest();
            if(i == 199) {
                long end = System.currentTimeMillis();
                System.out.println("--------------ScriptTestMultiTest:" + (end - begin) + "ms");
            }
        }
    }

    @Test
    public void scriptReleaseReadWriteLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ReleaseScript releaseScript = new ReleaseScript();
        List result = lettuceRedisLocker.execScript(releaseScript.script(), List.class, null,
                releaseScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path6"),
                releaseScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));


        result = lettuceRedisLocker.execScript(releaseScript.script(), List.class, null,
                releaseScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path6"),
                releaseScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 0, -1));
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        size = result.size();
        list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));
    }

    @Test
    public void scriptReleaseReEntrantLockTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ReleaseScript releaseScript = new ReleaseScript();
        long begin = System.currentTimeMillis();
        List result = lettuceRedisLocker.execScript(releaseScript.script(), List.class, null,
                releaseScript.scriptKey("coordinatorLeader", "LockWait", "Locked", "Path7"),
                releaseScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "threadId1", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id", 1, 0));
        long end = System.currentTimeMillis();
        System.out.println("--------------scriptReleaseMultiLockTest:" + (end - begin) + "ms");
        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));
    }

    @Test
    public void scriptDeleteNodeTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        DeleteFollowerScript deleteFollowerScript = new DeleteFollowerScript();
        List result = lettuceRedisLocker.execScript(deleteFollowerScript.script(), List.class, null,
                deleteFollowerScript.scriptKey("coordinatorLeader", "LockWait", "Locked"),
                deleteFollowerScript.scriptArgv("id", "192.168.137.1:7037-a572dd3f056e4bcebbf531b90939a964", "ReadSuffix", "WriteSuffix",
                        "WaitSuffix", "ReadParamSuffix", "WriteParamSuffix", "id"));

        System.out.println(result.get(0));
        System.out.println(result.get(1));
        int size = result.size();
        List<String> list = new ArrayList<>();
        for(int i = 2; i < size; i++) {
            list.add(result.get(i).toString());
        }
        System.out.println("notify can lock keys are:" + String.join(",", list));
    }

    @Test
    public void scriptCheckOneWaitPathTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        CheckOneWaitPathScript checkOneWaitPathScript = new CheckOneWaitPathScript();
        List result = lettuceRedisLocker.execScript(checkOneWaitPathScript.script(), List.class, null,
                checkOneWaitPathScript.scriptKey("coordinatorLeader", "path1"),
                checkOneWaitPathScript.scriptArgv("id", "192.168.137.1:4087-20b641ad2473476eb4d9b18f287e6c8e",
                        "1", "coordinatorLockWait", "get"));

        System.out.println(result.get(0));
        System.out.println(result.get(1));
    }

    @Test
    public void scriptRepairPathTest() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ResetPathScript resetPathScript = new ResetPathScript();
        List list = lettuceRedisLocker.execScript(resetPathScript.script(), List.class, null,
                resetPathScript.scriptKey("coordinatorLeader", "path1", "coordinatorLockWait", "coordinatorLockList"),
                resetPathScript.scriptArgv("id", "192.168.137.1:4087-c97665f297304b0daf723659971cd50d", "readLock", "writeLock", "coordinatorLockWait", "notifyWait", "readEnter",
                        "writeEnter", "id", 1, 2, 1, 1, "192.168.137.1:4087-20b641ad2473476eb4d9b18f287e6c8e-thread10-write", 1,
                        "192.168.137.1:4087-30b641ad2473476eb4d9b18f287e6c8f-thread12-read", -1,
                        "192.168.137.1:4087-40b641ad2473476eb4d9b18f287e6c8d-thread15-read", -1,
                        "192.168.137.1:4087-50b641ad2473476eb4d9b18f287e6c81-thread15-write",
                        "192.168.137.1:4087-60b641ad2473476eb4d9b18f287e6c82-thread15-write"));
        System.out.println(list.get(0).toString());
    }

    @Test
    public void scriptRepairPathTest2() throws Exception {
        LettuceRedisLocker<byte[], byte[]> lettuceRedisLocker = newLettuceRedisLocker(defaultCoordinatorConfig());
        ResetPathScript resetPathScript = new ResetPathScript();
        List list = lettuceRedisLocker.execScript(resetPathScript.script(), List.class, null,
                resetPathScript.scriptKey("coordinatorLeader", "path1", "coordinatorLockWait", "coordinatorLockList"),
                resetPathScript.scriptArgv("id", "192.168.137.1:4087-82e4eaa62a994389bd1a9817f7baba63", "readLock", "writeLock", "coordinatorLockWait", "notifyWait", "readEnter",
                        "writeEnter", "id", 0, 0, 0, 0));
        System.out.println(list.get(0).toString());
    }


    public static LettuceRedisLocker<byte[], byte[]> newLettuceRedisLocker(CoordinatorConfig config) {
        RedisAdvancedClusterAsyncCommandsImpl baseCommand = new RedisAdvancedClusterAsyncCommandsImpl(defaultConnection(), ByteArrayCodec.INSTANCE);
        com.xm.sanvanfo.interfaces.Serializer redisSerializer = defaultRedisSerializer();
        Serializer objSerializer = defaultObjSerializer();
        return new LettuceRedisLocker<>(config, baseCommand, redisSerializer, objSerializer);
    }

    public static CoordinatorConfig defaultCoordinatorConfig() {
        CoordinatorConfig config = new CoordinatorConfig();
        config.setNetCardName("eth4");
        config.setIpType(4);
        config.setAppName("CoordinatorTest");
        config.setLeaderKey("coordinatorLeader");
        config.setNodeListKey("coordinatorNodeList");
        config.setLeaderKeyTimeout(30000L);
        config.setElectionKey("coordinatorElection");
        config.setLockWaitSuffix("coordinatorLockWait");
        config.setLockListSuffix("coordinatorLockList");
        config.setIdleWaitMills(5000);
        config.setElectionTimeout(60000L);
        config.setZoneHour(8);
        config.setFailWaitMills(2000L);
        config.setRetryTimes(3);
        config.setClientTimeoutMills(20000L);
        config.setDeleteFollowerThread(3);
        config.setDeleteFollowerPrefix("coordinatorDeleteFollower");
        config.setMaxProcessWaitTime(300000L);
        config.setDetectDeadLock(true);
        config.setSpace("coordinator");
        config.setMaxResponseFailMills(3600*1000*6L);
        return config;
    }

    public static StatefulRedisClusterConnection<byte[], byte[]> defaultConnection() {
        List<RedisURI> list = new ArrayList<>();
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30001).build());
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30002).build());
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30003).build());
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30004).build());
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30005).build());
        list.add(RedisURI.builder().withHost("192.168.137.8").withPort(30006).build());
        RedisClusterClient redisClusterClient = RedisClusterClient.create(list);
        return  redisClusterClient.connect(ByteArrayCodec.INSTANCE);
    }

    public static Serializer defaultObjSerializer() {
        Serializer serializer = new Serializer() {
            @Override
            public <T> byte[] serialize(T t) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.writeValueAsBytes(t);
                }
                catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public <T> T deserialize(byte[] bytes, Class<T> clazz) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.readValue(bytes, clazz);
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        return serializer;
    }

    public static com.xm.sanvanfo.interfaces.Serializer defaultRedisSerializer() {
        com.xm.sanvanfo.interfaces.Serializer<byte[], byte[]> serializer = new com.xm.sanvanfo.interfaces.Serializer<byte[], byte[]>() {
            @Override
            public String toKeyString(byte[] bytes) {
                try {
                    return new String(bytes, "utf-8");
                }
                catch (UnsupportedEncodingException ex) {
                    throw new BusinessException(ex, "");
                }
            }

            @Override
            public byte[] toKey(String str) {
                try {
                    return str.getBytes("utf-8");
                }
                catch (UnsupportedEncodingException ex) {
                    throw new BusinessException(ex, "");
                }
            }

            @Override
            public byte[][] toKeyArray(String str) {
                try {
                    List<byte[]> list = new ArrayList<>();
                    ObjectMapper mapper = new ObjectMapper();
                    JavaType javaType = mapper.getTypeFactory().constructParametricType(ArrayList.class, String.class);
                    List<String> listString = mapper.readValue(str, javaType);
                    for (String ss:listString
                    ) {
                        list.add(toKey(ss));
                    }
                    return list.toArray(new byte[][] {});
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

            }

            @Override
            public String toKeyArrayString(List<byte[]> k) {
                List<String> list = new ArrayList<>();
                for (byte[] b:k
                ) {
                    list.add(toKeyString(b));
                }
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.writeValueAsString(list);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public byte[] toValue(String value) {
                try {
                    return value.getBytes("utf-8");
                }
                catch (UnsupportedEncodingException ex) {
                    throw new BusinessException(ex, "");
                }
            }

            @Override
            public byte[] toValue(byte[] value) {
                return value;
            }

            @Override
            public byte[] toValue(Object value) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsBytes(value);
                }
                catch (Exception ex) {
                    throw new BusinessException(ex, "toValue error");
                }
            }

            @Override
            public Object toObject(byte[] bytes, Class clazz) {
                try {
                    if(null == bytes) {
                        return null;
                    }
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(bytes, clazz);
                }
                catch (Exception ex) {
                    throw new BusinessException(ex, "toValue error");
                }
            }

            @Override
            public byte[] toValueBytes(byte[] bytes) {
                return bytes;
            }

            @Override
            public String toValueString(byte[] bytes) {
                try {
                    return new String(bytes, "utf-8");
                }
                catch (UnsupportedEncodingException ex) {
                    throw new BusinessException(ex, "");
                }
            }
        };

        return serializer;
    }
}
