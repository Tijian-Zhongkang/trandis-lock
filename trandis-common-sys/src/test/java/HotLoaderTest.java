import com.xm.sanvanfo.common.holdloader.classloader.HotClassLoaderUtils;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;

public class HotLoaderTest {
    @Test
    public void UrlTest() throws Exception {
        URL url = new URL("file:///D:\\work\\product/zk/new/src/ocrback/target");
        System.out.println(url.getPath());
        System.out.println(url.getFile());
        System.out.println(url.toString());

        url = new URL("http://www.baidu.com/work/product/zk/new/src/ocrback/target/8.txt");
        System.out.println(url.getPath());
        System.out.println(url.getFile());
        System.out.println(url.toString());
    }

    @Test
    public void classFolderLoader() throws Exception {

        HotClassLoaderUtils.addOrUpdateClassesPackage(
                new URL("file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\classes\\"),
                false, null
        );
        HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:\\work\\product\\zk\\new\\src\\cloudapi\\target\\classes\\"),
                false, null);

        HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:\\work\\product\\zk\\new\\src\\frameapi\\target\\classes\\"),
                false, null);

        HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:\\work\\product\\zk\\new\\src\\framecommon\\target\\classes\\"),
                false, null);

        HotClassLoaderUtils.addOrUpdateClassesPackage(new URL("file:///D:\\work\\product\\zk\\new\\src\\ocr-common\\target\\classes\\"),
                false, null);
        ClassLoader loader = HotClassLoaderUtils.getClassLoader(false);
        Class clazz = loader.loadClass("com.zk.tijian.ocrback.controller.AppointDateController");
        Field[] fields = clazz.getDeclaredFields();
        Optional.ofNullable(fields).ifPresent(o->{
            for (Field field:o
            ) {
                System.out.println(field.getName());
            }
        });
    }

    @Test
    public void classJarLoader() throws Exception {
        HotClassLoaderUtils.addOrUpdateJar(
                new URL("jar:file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\cloudapi-1.0-SNAPSHOT.jar!/"));
        HotClassLoaderUtils.addOrUpdateJar(
                new URL("jar:file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\frameapi-1.0.0.jar!/"));
        ClassLoader loader = HotClassLoaderUtils.getClassLoader(false);
        Class clazz = loader.loadClass("com.zk.cloud.cloudapi.dto.DataChannelPackageDTO");
        HashSet<Class> set = new HashSet<>();
        ReflectUtils.getAssociatedClass(clazz, set);

        printClassInfo(clazz);
    }

    @Test
    public void classJarLoaderDelete() throws Exception {
        classJarLoader();
        HotClassLoaderUtils.deleteJar(
                new URL("jar:file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\frameapi-1.0.0.jar!/"));
        ClassLoader loader = HotClassLoaderUtils.getClassLoader(false);
        Class clazz = loader.loadClass("com.zk.cloud.cloudapi.dto.DataChannelPackageDTO");
        printClassInfo(clazz);
    }

    @Test
    public void classJarLoaderUpdate() throws Exception {
        classJarLoader();
        HotClassLoaderUtils.deleteJar(
                new URL("jar:file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\frameapi-1.0.0.jar!/"));
        ClassLoader loader = HotClassLoaderUtils.getClassLoader(false);
        try {
            loader.loadClass("com.zk.cloud.cloudapi.dto.DataChannelPackageDTO");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        HotClassLoaderUtils.addOrUpdateJar(
                new URL("jar:file:///D:\\work\\product\\zk\\new\\src\\ocrback\\target\\frameapi-1.0.0.jar!/"));
        loader = HotClassLoaderUtils.getClassLoader(false);
        Class clazz = loader.loadClass("com.zk.cloud.cloudapi.dto.DataChannelPackageDTO");
        printClassInfo(clazz);
    }

    private void printClassInfo(Class clazz) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        Optional.ofNullable(fields).ifPresent(o->{
            for (Field field:o
            ) {
                System.out.println(field.getName());
            }
        });
        Method[] methods = clazz.getDeclaredMethods();
        Optional.ofNullable(methods).ifPresent(o->{
            for (Method method:o) {
                System.out.println(method.toGenericString());
            }
        });
    }
}
