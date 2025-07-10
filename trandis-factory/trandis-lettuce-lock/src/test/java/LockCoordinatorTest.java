import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.xm.sanvanfo.*;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.compress.CompressorFactory;
import com.xm.sanvanfo.trandiscore.compress.CompressorType;
import com.xm.sanvanfo.trandiscore.netty.config.NettyClientConfig;
import com.xm.sanvanfo.trandiscore.netty.config.NettyServerConfig;
import com.xm.sanvanfo.trandiscore.netty.constrant.TransportServerType;
import com.xm.sanvanfo.trandiscore.netty.plugins.IServerBootstrapPlugin;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.util.Scanner;


public class LockCoordinatorTest {

    private final boolean run = true;

    @Test
    public void electionTest7037() throws Exception {
        electionTest(7037);
    }

    @Test
    public void electionTest7038() throws Exception {
        electionTest(7038);
    }

    @Test
    public void electionTest7039() throws Exception {
        electionTest(7039);
    }

    @Test
    public void electionTest7040() throws Exception {
        electionTest(7040);
    }

    @Test
    public void electionTest7041() throws Exception {
        electionTest(7041);
    }

    @Test
    public void elctionAndClose() throws Exception {
        LockCoordinatorInit(7042);
        LockCoordinator.INSTANCE().close();
    }

    private void electionTest(int port) throws Exception {
        if(run) {
            LockCoordinatorInit(port);
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String line = scanner.next();
                if (line.equals("q")) {
                    break;
                }
            }
            scanner.close();
        }
    }

    public static void LockCoordinatorInit(int port) throws Exception {
        CoordinatorConfig config = LettuceRedisRedisLockerTest.defaultCoordinatorConfig();
        IRedisLocker locker = LettuceRedisRedisLockerTest.newLettuceRedisLocker(config);
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

}
