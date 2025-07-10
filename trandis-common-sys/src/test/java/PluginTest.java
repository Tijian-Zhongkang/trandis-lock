import com.xm.sanvanfo.common.holdloader.classloader.HotClassLoaderUtils;
import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.common.plugin.PluginLoader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;

public class PluginTest {

    @Test
    public void pluginNameTest() throws Exception {
        PluginLoader.INSTANCE().registerPlugin(TestPlugin.class.getName());
        PluginLoader.INSTANCE().registerPlugin(TestNoAnnotationPlugin.class.getName());
        TestInterface testInterface = PluginLoader.INSTANCE().load(TestInterface.class, "annotation");
        testInterface.test();
        testInterface = PluginLoader.INSTANCE().load(TestInterface.class, TestNoAnnotationPlugin.class.getCanonicalName());
        testInterface.test();
    }

    @Test
    public void pluginInstanceTest() throws Exception {
        IPlugin annotation  = new TestPlugin();
        IPlugin noAnnotation = new TestNoAnnotationPlugin();
        PluginLoader.INSTANCE().registerPlugin(annotation);
        PluginLoader.INSTANCE().registerPlugin(noAnnotation);

        TestInterface testInterface = PluginLoader.INSTANCE().load(TestInterface.class, "annotation");
        testInterface.test();
        testInterface = PluginLoader.INSTANCE().load(TestInterface.class, TestNoAnnotationPlugin.class.getCanonicalName());
        testInterface.test();
    }

    @Test
    public void pluginOuterJarTest() throws Exception {
        HotClassLoaderUtils.addOrUpdateJar(new URL("file:///D:\\work\\product\\form\\spring-form\\spring-dynamic-httpproxy-dto\\target\\spring-dynamic-httpproxy-dto-1.0-SNAPSHOT.jar"));
        PluginLoader.INSTANCE().scanOuterPluginJarURL(new URL("file:///D:\\work\\product\\form\\spring-form\\spring-dynamic-httpproxy\\target\\spring-dynamic-httpproxy-1.0-SNAPSHOT.jar"), null, null, true, null, null);
    }

    @Test
    public void pluginOuterPackageTest() throws Exception {
        HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:\\work\\product\\form\\spring-form\\spring-dynamic-httpproxy-dto\\target\\classes"), false, null);
        PluginLoader.INSTANCE().scanOuterPluginPackageURL(new URL("file:///D:\\work\\product\\form\\spring-form\\spring-dynamic-httpproxy\\target\\classes"), null, null, true, null, null, false, "");
    }

    public  interface TestInterface {
        void test();
    }

    @CustomPlugin(registerClass = TestInterface.class, name = "annotation")
    public static class TestPlugin implements IPlugin, TestInterface {

        @Override
        public void test() {
            System.out.println("this is plugin");
        }
    }

    public static class TestNoAnnotationPlugin implements IPlugin, TestInterface {

        @Override
        public void test() {
            System.out.println("this is no annotation plugin");
        }
    }
}
