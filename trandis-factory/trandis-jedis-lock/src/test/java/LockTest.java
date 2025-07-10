import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.xm.sanvanfo.*;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.lock.TReentrantLock;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.CompressorFactory;
import com.xm.sanvanfo.trandiscore.compress.CompressorType;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.constrant.TransportServerType;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockTest {

    @Test
    public void TReentrantLockTest() throws Exception {
        LockCoordinatorInit(1087);
        TReentrantLock lock = new TReentrantLock("path1");
        lock.acquire();
        Thread.sleep(1000);
        lock.release();
        LockCoordinator.INSTANCE().close();
    }

    public static void LockCoordinatorInit(int port) throws Exception {
        CoordinatorConfig config = defaultCoordinatorConfig();
        IRedisLocker locker = defaultLocker(config);
        LockCoordinator.INSTANCE().setLocker(locker);
        LockCoordinator.INSTANCE().configuration(config);
        NettyClientConfig nettyClientConfig = nettyClientConfig();
        NettyServerConfig nettyServerConfig = nettyServerConfig(port);
        LockCoordinator.INSTANCE().setNettyClientConfig(nettyClientConfig, null);
        PluginLoader.INSTANCE().registerPlugin(new RedisCoordinatorServerPlugin());
        PluginLoader.INSTANCE().registerPlugin(new ServerSendRequestTimeoutPlugin());
        IServerBootstrapPlugin plugin = PluginLoader.INSTANCE().load(IServerBootstrapPlugin.class, "redisCoordinator");
        LockCoordinator.INSTANCE().setNettyServerConfig(nettyServerConfig, new IServerBootstrapPlugin[]{plugin});
        LockCoordinator.INSTANCE().setCompressor(CompressorFactory.getCompressor(CompressorType.NONE.getCode()));
        ObjectMapper objectMapper= JsonMapper.builder().configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true).build();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        LockCoordinator.INSTANCE().setSerializer(new JacksonSerializer(objectMapper));
        LockCoordinator.INSTANCE().startWaitSuccess();
    }

    public static NettyClientConfig nettyClientConfig() {
        NettyClientConfig clientConfig = new NettyClientConfig();
        clientConfig.setNetworkCardName("eth4");
        clientConfig.setIpType(4);
        clientConfig.setServerId("LockCoordinatorServer");
        clientConfig.setClientWorkerThreads(2);
        clientConfig.setBOSS_THREAD_PREFIX("boss");
        clientConfig.setWORKER_THREAD_PREFIX("worker");
        clientConfig.setSHARE_BOSS_WORKER(false);
        clientConfig.setWORKER_THREAD_SIZE(2);
        clientConfig.setMAX_WRITE_IDLE_SECONDS(5);
        clientConfig.setCONFIGURED_CODEC((byte)1);
        clientConfig.setCONFIGURED_COMPRESSOR((byte)0);
        clientConfig.setClientChannelClazz(NioSocketChannel.class);
        return clientConfig;
    }

    public static NettyServerConfig nettyServerConfig(int port) {
        NettyServerConfig serverConfig = new NettyServerConfig();
        serverConfig.setNetworkCardName("eth4");
        serverConfig.setIpType(4);
        serverConfig.setServerSelectorThreads(1);
        serverConfig.setServerSocketSendBufSize(1024);
        serverConfig.setServerSocketResvBufSize(1024);
        serverConfig.setServerWorkerThreads(1);
        serverConfig.setSoBackLogSize(50);
        serverConfig.setWriteBufferHighWaterMark(65536);
        serverConfig.setWriteBufferLowWaterMark(32768);
        serverConfig.setBossThreadSize(1);
        serverConfig.setListenPort(port);
        serverConfig.setMinServerPoolSize(2);
        serverConfig.setMaxServerPoolSize(20);
        serverConfig.setMaxTaskQueueSize(256);
        serverConfig.setKeepAliveTime(10);
        serverConfig.setServerShutdownWaitTime(10);
        serverConfig.setServerAppName("LockCoordinatorServer");
        serverConfig.setBOSS_THREAD_PREFIX("boss");
        serverConfig.setWORKER_THREAD_PREFIX("worker");
        serverConfig.setSHARE_BOSS_WORKER(false);
        serverConfig.setWORKER_THREAD_SIZE(2);
        serverConfig.setMAX_WRITE_IDLE_SECONDS(20);
        serverConfig.setCONFIGURED_CODEC((byte)1);
        serverConfig.setCONFIGURED_COMPRESSOR((byte)0);
        serverConfig.setTRANSPORT_SERVER_TYPE(TransportServerType.NATIVE);
        serverConfig.setSERVER_CHANNEL_CLAZZ(NioServerSocketChannel.class);
        return serverConfig;
    }

    public static JedisRedisLocker defaultLocker(CoordinatorConfig config) {
        return new JedisRedisLocker(config, defaultJedis(), defaultRedisSerializer(), defaultObjSerializer());
    }

    public static JedisCluster defaultJedis() {
        Set<HostAndPort> sets = new HashSet<>();
        sets.add(new HostAndPort("192.168.137.8", 30001));
        sets.add(new HostAndPort("192.168.137.8", 30002));
        sets.add(new HostAndPort("192.168.137.8", 30003));
        sets.add(new HostAndPort("192.168.137.8", 30004));
        sets.add(new HostAndPort("192.168.137.8", 30005));
        sets.add(new HostAndPort("192.168.137.8", 30006));
        return new JedisCluster(sets);
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

    public static com.xm.sanvanfo.interfaces.Serializer<byte[], byte[]> defaultRedisSerializer() {
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
