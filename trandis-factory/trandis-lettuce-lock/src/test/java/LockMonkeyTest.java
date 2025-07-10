import com.xm.sanvanfo.IMonkeyPlugin;
import com.xm.sanvanfo.common.holdloader.classloader.HotClassLoaderUtils;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import com.xm.sanvanfo.lock.TReentrantLock;
import com.xm.sanvanfo.netty.FollowerNettyRemotingClient;
import com.xm.sanvanfo.netty.LeaderNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.NettyChannel;
import com.xm.sanvanfo.trandiscore.netty.NettyPoolKey;
import com.xm.sanvanfo.trandiscore.netty.NettyServerBootstrap;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class LockMonkeyTest {

    private final boolean run = true;

    @CustomPlugin(registerClass = IMonkeyPlugin.class, name = "testPlugin")
    public static class MonkeyPluginTest implements IMonkeyPlugin {

        @Override
        public void before(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType) {
            System.out.println("plugin before:" + name);
            System.out.println(name + " parameter:" + parameters.stream().map(this::toString).collect(Collectors.joining(",")));
        }

        @Override
        public Object after(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType, Object returnValue) {
            System.out.println("plugin after:" + name);
            System.out.println(name + " parameter:" + parameters.stream().map(this::toString).collect(Collectors.joining(",")));
            return returnValue;
        }

        private String toString(Object o) {
            if(null == o) {
                return "null";
            }
            return o.toString();
        }
    }

    @Test
    public void loadClassTest() throws Exception {
        if(run) {
            PluginLoader.INSTANCE().registerPlugin(new MonkeyPluginTest());
            HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:/work/product/sanvanfo/src/trandis-agent/trandis-monkey-agent/target/test-classes/"), false, null);
            Class clazz = HotClassLoaderUtils.getClassLoader(false).loadClass("MonkeyAgentTest$TestClass");
            Object obj = clazz.newInstance();
            Object set = obj.getClass().getDeclaredMethod("getTest").invoke(obj);
            System.out.println(set);
        }

    }

    @Test
    public void MonkeyTReentrantLockTest() throws Exception {
        if(run) {
            PluginLoader.INSTANCE().registerPlugin(new MonkeyPluginTest());
            LockCoordinatorTest.LockCoordinatorInit(4087);
            TReentrantLock lock = new TReentrantLock("path1");
            lock.acquire();
            Thread.sleep(1000);
            lock.release();
        }
    }

    @CustomPlugin(registerClass = IMonkeyPlugin.class, name = "clientTcpClosePlugin")
    public static class ClientTcpClosePlugin implements IMonkeyPlugin {

        @Override
        public void before(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType) {

        }

        @Override
        public Object after(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType, Object returnValue) {
            if(thisObj instanceof FollowerNettyRemotingClient && name.equals("start")) {
                try {

                    Method method = FollowerNettyRemotingClient.class.getDeclaredMethod("closeChannel", NettyChannel.class);
                    method.setAccessible(true);
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        try {
                            while (true) {
                                Random random = new Random();
                                int r = random.nextInt() % 10;
                                int time = (int) (10 + 10 * (r / 10.0));
                                Thread.sleep(time * 1000);
                                Field field = thisObj.getClass().getDeclaredField("nettyChannel");
                                field.setAccessible(true);
                                NettyChannel channel = (NettyChannel) field.get(thisObj);
                                if(null == channel) {
                                    continue;
                                }
                                method.invoke(thisObj, channel);
                                break;
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return returnValue;
        }
    }

    //-javaagent:D:/trandis-monkey-agent-1.0.jar={\"pluginName\":\"clientTcpClosePlugin\",\"transformerList\":[\"com.xm.sanvanfo.netty.FollowerNettyRemotingClient\"]}
    @Test
    public void MonkeyClientTcpCloseTest() throws Exception {
        if(run) {
            PluginLoader.INSTANCE().registerPlugin(new ClientTcpClosePlugin());
            LockCoordinatorTest.LockCoordinatorInit(5087);
            List<TReentrantLock> list = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                list.add(new TReentrantLock("path" + i));
            }
            long begin = System.currentTimeMillis();
            for (TReentrantLock lock : list
            ) {
                lock.acquire();
                Thread.sleep(30);
            }
            long end = System.currentTimeMillis();
            System.out.println("---------------------acquire" + (end - begin) + "ms");
            for (TReentrantLock lock : list
            ) {
                lock.release();
            }
            long nexEnd = System.currentTimeMillis();
            System.out.println("---------------------release" + (nexEnd - end) + "ms");
        }
    }

    @CustomPlugin(registerClass = IMonkeyPlugin.class, name = "serverTcpClosePlugin")
    public static class ServerTcpClosePlugin implements IMonkeyPlugin {

        private final Map<NettyPoolKey, NettyChannel> clientList = new ConcurrentHashMap<>();
        @Override
        public void before(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType) {

        }

        @Override
        public Object after(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType, Object returnValue) {
            if(thisObj instanceof LeaderNettyRemotingServer) {
                if(name.equals("start")) {
                    try {
                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        Method method = ReflectUtils.getDeclareMehodByName(LeaderNettyRemotingServer.class, "closeChannel", NettyChannel.class);
                        method.setAccessible(true);
                        executorService.execute(() -> {
                            try {
                                do {
                                    Random random = new Random();
                                    int r = random.nextInt() % 10;
                                    int time = (int) (10 + 10 * (r / 10.0));
                                    Thread.sleep(time * 1000);
                                    for (NettyChannel channel : clientList.values()
                                    ) {
                                        method.invoke(thisObj, channel);
                                    }
                                } while (clientList.size() <= 0);

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                else if(name.equals("updateChannel")) {
                    NettyChannel channel = (NettyChannel) returnValue;
                    clientList.put(channel.getKey(), channel);
                }

            }
            return returnValue;
        }
    }

    //-javaagent:D:/trandis-monkey-agent-1.0.jar={\"pluginName\":\"serverTcpClosePlugin\",\"transformerList\":[\"com.xm.sanvanfo.netty.LeaderNettyRemotingServer\",\"com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer\"]}
    @Test
    public void MonkeyServerTcpCloseTest() throws Exception {
        if(run) {
            PluginLoader.INSTANCE().registerPlugin(new ServerTcpClosePlugin());
            LockCoordinatorTest.LockCoordinatorInit(5087);
            List<TReentrantLock> list = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                list.add(new TReentrantLock("path" + i));
            }
           Thread.sleep(5000);
            for (TReentrantLock lock : list
            ) {
                lock.acquire();
                Thread.sleep(30);
                lock.release();
            }
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

}
