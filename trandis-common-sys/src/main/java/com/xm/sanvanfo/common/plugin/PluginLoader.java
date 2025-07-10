package com.xm.sanvanfo.common.plugin;

import com.xm.sanvanfo.common.Disposable;
import com.xm.sanvanfo.common.holdloader.classloader.HotClassLoaderUtils;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.LruThreadSafeGetter;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
@Slf4j
public class PluginLoader implements Disposable {

    private static final PluginLoader pluginLoader = new PluginLoader();
    private final ConcurrentHashMap<Class, ConcurrentHashMap<String, PluginHolder>> pluginMaps = new ConcurrentHashMap<>();
    private final Map<String, Set<PluginDefinition>> scanMaps = new ConcurrentHashMap<>();
    private final LruThreadSafeGetter<String, Object> locks = new LruThreadSafeGetter<>(3600L * 24, 30L, Object::new);
    public static PluginLoader INSTANCE() {
        return pluginLoader;
    }


    @Override
    public void dispose() {
        locks.dispose();
    }

    public void registerPlugin(String className, Consumer<PluginDefinition> consumer) throws ClassNotFoundException {
        Class clazz = Class.forName(className);
        registerPluginInner(clazz, null, consumer);
    }

    public void registerPlugin(String className) throws ClassNotFoundException {
        registerPlugin(className, null);
    }

    public void registerPlugin(IPlugin obj, Consumer<PluginDefinition> consumer) {
        Class clazz = obj.getClass();
        registerPluginInner(clazz, obj, consumer);
    }

    public void registerPlugin(IPlugin obj) {
        registerPlugin(obj, null);
    }

    public void cancelPlugin(String className, Consumer<PluginDefinition> consumer) throws ClassNotFoundException {
        Class clazz = Class.forName(className);
        cancelPluginInner(clazz, consumer);
    }

    public void cancelPlugin(String className) throws ClassNotFoundException  {
        cancelPlugin(className, null);
    }

    public void cancelPlugin(IPlugin obj, Consumer<PluginDefinition> consumer) {
        Class clazz = obj.getClass();
        cancelPluginInner(clazz, consumer);
    }

    public void cancelPlugin(IPlugin obj) {
        cancelPlugin(obj, null);
    }

    public void scanOuterPluginJarURL(URL url, List<URL> depends, Set<Class> filter, Boolean autoAssociationFilter, Consumer<PluginDefinition> consumer, Consumer<Class> consumerClazz) throws Exception {
        String urlStr = wrap(url);
        URL newURL = new URL(urlStr);
        removeOuterPluginJarURL(newURL);
        HotClassLoaderUtils.addOrUpdateBundleJar(newURL);
        if(null != depends) {
            HotClassLoaderUtils.addBundleDepends(newURL, depends);
        }
        URLConnection connection = newURL.openConnection();
        Asserts.isTrue(connection instanceof JarURLConnection);
        try(JarFile file = ((JarURLConnection)connection).getJarFile()) {
            Set<PluginDefinition> set = scanMaps.computeIfAbsent(newURL.toString(), o -> new HashSet<>());
            Enumeration<JarEntry> enumeration = file.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                String fileName = entry.getName();
                registerClassFile(url, fileName, set, filter, autoAssociationFilter, consumerClazz);
            }
            if (null != consumer) {
                for (PluginDefinition definition : set
                ) {
                    consumer.accept(definition);
                }
            }
        }
    }

    public void removeOuterPluginJarURL(URL url) throws Exception {
        String urlStr = wrap(url);
        URL newURL = new URL(urlStr);
        HotClassLoaderUtils.deleteBundleJar(newURL);
        removeOuterPluginMap(newURL);
    }

    public void scanOuterPluginPackageURL(URL url, List<URL> depends, Set<Class> filter, Boolean autoAssociationFilter, Consumer<PluginDefinition> consumer, Consumer<Class> consumerClazz, Boolean encrypt, String privateKey) throws Exception {
        String urlStr = url.toString();
        if(!urlStr.endsWith("/")) {
            urlStr = urlStr + "/";
        }
        URL newURL = new URL(urlStr);
        removeOuterPluginPackageURL(newURL);
        HotClassLoaderUtils.addOrUpdateBundlePackage(newURL, encrypt, privateKey);
        if(null != depends) {
            HotClassLoaderUtils.addBundleDepends(newURL, depends);
        }
        Set<PluginDefinition> set = scanMaps.computeIfAbsent(newURL.toString(), o-> new HashSet<>());
        Asserts.isTrue(newURL.getProtocol().equalsIgnoreCase("file"));
        File directory  = new File(newURL.getPath());
        List<String> files = new ArrayList<>();
        scanDirectory("", directory, files);
        for (String fileName:files
             ) {
            registerClassFile(url, fileName, set, filter, autoAssociationFilter, consumerClazz);
        }
        if(null != consumer) {
            for (PluginDefinition definition : set
            ) {
                consumer.accept(definition);
            }
        }
    }

    public void removeOuterPluginPackageURL(URL url) throws Exception {
        String urlStr = url.toString();
        if(!urlStr.endsWith("/")) {
            urlStr = urlStr + "/";
        }
        URL newURL = new URL(urlStr);
        Asserts.isTrue(newURL.getProtocol().equalsIgnoreCase("file"));
        HotClassLoaderUtils.deleteBundlePackage(newURL);
        removeOuterPluginMap(newURL);
    }


    @SuppressWarnings({"unchecked"})
    public <T> T load(Class clazz, String activateName) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
       synchronized (getLock(clazz, activateName)) {
           PluginHolder holder = loadInHolder(clazz, activateName);
           T t = (T)holder.getPlugin();
           if(null != t) {
               return t;
           }
           Class cls = holder.getClazz();
           Constructor constructor = cls.getConstructor();
           t = (T)constructor.newInstance();
           holder.setPlugin((IPlugin) t);
           return t;
       }
    }

    @SuppressWarnings({"unchecked"})
    public <T> T load(Class clazz, String activateName, Class[] classes, Object[] args) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException{
        if(null == args || args.length == 0) {
            return load(clazz, activateName);
        }
        synchronized (getLock(clazz, activateName)) {
            PluginHolder holder = loadInHolder(clazz, activateName);
            T t = (T)holder.getPlugin();
            if(null != t) {
                return t;
            }
            Class cls = holder.getClazz();
            Class[] argTypes = classes;
            if(null == argTypes) {
                argTypes = new Class[args.length];
                for(int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
            }
            Constructor constructor = cls.getConstructor(argTypes);
            t = (T)constructor.newInstance(args);
            holder.setPlugin((IPlugin) t);
            return t;
        }
    }

    public <T> T load(Class clazz, String activateName, Object[] args) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException{

        List<Object> list =  Arrays.asList(args);
        Class[] classes = null;
        if(list.size() > 0) {
            classes = list.stream().filter(Objects::nonNull).map(Object::getClass).collect(Collectors.toList()).toArray(new Class[] {});
        }
        args = list.stream().filter(Objects::nonNull).toArray();
        return classes == null ? load(clazz, activateName) : load(clazz, activateName, classes, args);
    }

    public void unload(Class clazz, String activateName) {
        synchronized (getLock(clazz, activateName)) {
            ConcurrentHashMap<String, PluginHolder> plugins = pluginMaps.get(clazz);
            if(null == plugins) {
                return;
            }
            PluginHolder pluginLoader = plugins.get(activateName);
            if(null == pluginLoader) {
                return;
            }
            pluginLoader.setPlugin(null);
        }
    }

    public Map<String, PluginHolder> getPlugins(Class clazz) {
        ConcurrentHashMap<String, PluginHolder> map = pluginMaps.get(clazz);
        if(null == map) {
            return null;
        }
        return Collections.unmodifiableMap(map);
    }

    private String wrap(URL url) throws PluginException {
        String urlStr = url.toString();
        if(!urlStr.endsWith(".jar") && !urlStr.endsWith(".jar!/")) {
            throw new PluginException("url path must ends with .jar or .jar!/");
        }
        if(urlStr.endsWith(".jar")) {
            urlStr =  urlStr + "!/";
        }
        if(!urlStr.startsWith("jar:")) {
            urlStr = "jar:" + urlStr;
        }
        return urlStr;
    }

    private void registerClassFile(URL url, String fileName, Set<PluginDefinition> set, Set<Class> filter, Boolean autoAssociationFilter, Consumer<Class> consumer) throws Exception {
        if(fileName.endsWith(".class")) {
            String className = fileName.split("\\.")[0].replaceAll("/", "\\.");
            if(autoAssociationFilter) {
                String[] paths = className.split("\\.");
                int filterLen = Math.min(paths.length - 1, 3);
                List<String> list = new ArrayList<>(Arrays.asList(paths).subList(0, filterLen));
                HotClassLoaderUtils.addAssociateFilters(Collections.singletonList(String.join(".", list)));
            }
            ClassLoader loader = HotClassLoaderUtils.getBundleClassloader(url);
            if(null == loader) {
                log.warn("bundle class loader is not loaded bundle url:{}", url.toString());
                return;
            }
            Class<?> clazz = loader.loadClass(className);
            if(null != consumer) {
                consumer.accept(clazz);
            }
            CustomPlugin plugin = clazz.getAnnotation(CustomPlugin.class);
            if(null != plugin) {
                Class pluginClass =  plugin.registerClass();
                if(null != filter && !filter.contains(pluginClass)) {
                    return;
                }
                if(pluginClass != void.class) {
                    String name =  plugin.name();
                    name = StringUtils.isEmpty(name) ? clazz.getCanonicalName() : name;
                    synchronized (getLock(clazz, name)) {
                        PluginDefinition definition = new PluginDefinition(clazz, pluginClass, name);
                        set.add(definition);
                        checkAndRegisterPluginInner(pluginClass, name, clazz, null, plugin.note());
                    }
                }
            }
        }
    }

    private void scanDirectory(String path, File directory, List<String> files) {
        if(StringUtils.isNotEmpty(path)) {
            path = path + "/";
        }
        File[] list = directory.listFiles();
        if(null != list) {
            for (File file : list
            ) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    files.add(path + file.getName());
                }
                else if(!file.isFile()) {
                    scanDirectory(path +  file.getName(), file, files);
                }
            }
        }
    }

    private void removeOuterPluginMap(URL newURL) {
        Set<PluginDefinition> set = scanMaps.get(newURL.toString());
        if(null != set) {
            set.forEach(o->{
                synchronized (getLock(o.registerClass, o.name)) {
                    ConcurrentHashMap<String, PluginHolder> plugins = pluginMaps.get(o.registerClass);
                    if(null != plugins) {
                        plugins.remove(o.name);
                    }
                }
            });
            scanMaps.remove(newURL.toString());
        }
    }

    private void registerPluginInner(Class clazz, IPlugin obj, Consumer<PluginDefinition> consumer) {
        List<String> strings = new ArrayList<>();
        Class cls = getRegisterClass(clazz, strings);
        String activateName = strings.get(0);
        synchronized (getLock(cls, activateName)) {
            checkAndRegisterPluginInner(cls, activateName, clazz, obj, strings.get(1));
            if(null != consumer) {
                consumer.accept(new PluginDefinition(clazz, cls, activateName));
            }
        }
    }

    private void checkAndRegisterPluginInner(Class cls, String activateName, Class clazz, IPlugin obj, String str) {
        ConcurrentHashMap<String, PluginHolder> plugins = pluginMaps.putIfAbsent(cls, new ConcurrentHashMap<>());
        if(plugins == null) {
            plugins = pluginMaps.get(cls);
        }
        if(plugins.containsKey(activateName)) {
            throw new PluginException(String.format("当前已经注册了：%s", activateName));
        }
        plugins.put(activateName, new PluginHolder(clazz, obj, str));
    }

    private void cancelPluginInner(Class clazz, Consumer<PluginDefinition> consumer) {

        List<String> strings = new ArrayList<>();
        Class cls = getRegisterClass(clazz, strings);
        String activateName = strings.get(0);
        synchronized (getLock(cls, activateName)) {
            ConcurrentHashMap<String, PluginHolder> plugins = pluginMaps.get(cls);
            if(null == plugins) {
                return;
            }
            plugins.remove(activateName);
            if(plugins.size() == 0) {
                pluginMaps.remove(cls);
            }
            if(null != consumer) {
                consumer.accept(new PluginDefinition(clazz, cls, activateName));
            }
        }
    }

    private PluginHolder loadInHolder(Class clazz, String activateName) {
        ConcurrentHashMap<String, PluginHolder> plugins = pluginMaps.get(clazz);
        if(null == plugins) {
            throw new PluginNoFoundException("plugin has not register!");
        }
        PluginHolder pluginLoader = plugins.get(activateName);
        if(null == pluginLoader) {
            throw new PluginNoFoundException("plugin has not register!");
        }
        return pluginLoader;
    }

    private Class getRegisterClass(Class clazz, List<String> strings) {
        Annotation plugin = clazz.getAnnotation(CustomPlugin.class);
        if(null != plugin) {
            if(plugin instanceof CustomPlugin) {
                Class pluginClass =  ((CustomPlugin) plugin).registerClass();
                if(pluginClass != void.class) {
                    String name = ((CustomPlugin) plugin).name();
                    strings.add(StringUtils.isEmpty(name) ? clazz.getCanonicalName() : name);
                    strings.add(((CustomPlugin) plugin).note());
                    return pluginClass;
                }
            }
        }
        strings.add(clazz.getCanonicalName());
        strings.add("");
        Class<?>[] interfaces = ReflectUtils.getInterfaces(clazz);
        for (Class<?> interf:interfaces
             ) {
            if(!interf.equals(IPlugin.class)) {
                return interf;
            }
        }
        return clazz;
    }

    private Object getLock(Class clazz, String activateName) {
        String key = String.format("%s-%s", clazz.getCanonicalName(), activateName);
         return locks.get(key);
    }


    @SuppressWarnings("WeakerAccess")
    @AllArgsConstructor
    @Data
    public static class PluginHolder {
        private Class clazz;
        private IPlugin plugin;
        private String note;
    }

    @AllArgsConstructor
    @Data
    public static class PluginDefinition {
        private Class clazz;
        private Class registerClass;
        private String name;
    }
}
