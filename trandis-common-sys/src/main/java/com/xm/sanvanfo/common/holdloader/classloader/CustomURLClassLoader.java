package com.xm.sanvanfo.common.holdloader.classloader;

import com.xm.sanvanfo.common.holdloader.classloader.urlloader.CustomURLClassPath;
import com.xm.sanvanfo.common.holdloader.classloader.urlloader.ErrorVersionException;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import sun.misc.Resource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CustomURLClassLoader extends URLClassLoader implements Cloneable {


    protected URLStreamHandlerFactory factory;
    protected ClassLoader parent;
    private  final ConcurrentHashMap<String, URL> loadedClass = new ConcurrentHashMap<>();
    protected final Set<String> associateFilters = Collections.synchronizedSet(new HashSet<>());
    public CustomURLClassLoader(URL[] urls, Set<String> associateFilters, ClassLoader parent) throws Exception{
        super(urls, parent);
        this.parent = parent;
        if(null != associateFilters)
            this.associateFilters.addAll(associateFilters);
        setPrivateProperty("ucp", new CustomURLClassPath(urls));
    }

    public CustomURLClassLoader(URL[] urls, Set<String> associateFilters) throws Exception {
        super(urls);
        if(null != associateFilters)
            this.associateFilters.addAll(associateFilters);
        setPrivateProperty("ucp", new CustomURLClassPath(urls));
    }

    public CustomURLClassLoader(URL[] urls, Set<String> associateFilters,  ClassLoader parent,
                                URLStreamHandlerFactory acc) throws Exception {
        super(urls, parent, acc);
        setPrivateProperty("ucp", new CustomURLClassPath(urls));
        factory = acc;
        this.parent = parent;
        if(null != associateFilters)
            this.associateFilters.addAll(associateFilters);
    }

    public void addAssociateFilters(List<String> list) {
        if(null != list)
           this.associateFilters.addAll(list);
    }

    public void deleteCustomURL(URL file) throws Exception {
        CustomURLClassPath ucp = (CustomURLClassPath)getPrivateProperty("ucp");
        ucp.deleteURL(file);
    }

    public void addCustomURL(URL file) throws Exception {
        CustomURLClassPath ucp = (CustomURLClassPath)getPrivateProperty("ucp");
        ucp.addURL(file);
    }

    @Override
    public  Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException  {
        Class cls = super.loadClass(name, resolve);
        return checkInDeleteClass(cls, name);
    }

    public Object clone(Map<URL, URL> replace, List<URL> add) throws CloneNotSupportedException {
        try {
            CustomURLClassLoader loader;
            URLStreamHandlerFactory acc = factory;
            URL[] newUrls = mergeURLs(this.getURLs(), replace, add);
            if(null != acc) {
                Constructor<? extends CustomURLClassLoader> constructor = this.getClass().getConstructor(URL[].class, Set.class, ClassLoader.class, URLStreamHandlerFactory.class);
                loader = constructor.newInstance(newUrls, associateFilters, this.getParent(), acc);
            }
            else if(parent != null) {
                Constructor<? extends CustomURLClassLoader> constructor = this.getClass().getConstructor(URL[].class, Set.class, ClassLoader.class);
                loader = constructor.newInstance(newUrls, associateFilters, this.getParent());
            }
            else {
                Constructor<? extends CustomURLClassLoader> constructor = this.getClass().getConstructor(URL[].class, Set.class);
                loader = constructor.newInstance(newUrls, associateFilters);
            }
            return loader;
        }
        catch (Exception ex) {
            throw new CloneNotSupportedException(ex.getMessage());
        }
    }

    @Override
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException
    {
        final Class<?> result;
        try {
            CustomURLClassPath ucp = (CustomURLClassPath) getPrivateProperty("ucp");
            AccessControlContext acc = (AccessControlContext)getPrivateProperty("acc");
            Object o = this;
            result = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Class<?>>) () -> {
                        String path = name.replace('.', '/').concat(".class");
                        Resource res = ucp.getResource(path, false);
                        if (res != null) {
                            try {
                                Class c = defineClass(o, name, res);
                                if(!loadedClass.containsKey(name)) {
                                    loadedClass.put(name, res.getURL());
                                }
                                return c;
                            }
                            catch (Exception ex) {
                                throw new ClassNotFoundException(name, ex);
                            }
                        } else {
                            return null;
                        }
                    }, acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
        catch (Exception ex) {
            throw new ClassNotFoundException(ex.getMessage());
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    private Class<?> defineClass( Object o, String name, Resource res) throws  Exception {
        long t0 = System.nanoTime();
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Manifest man = res.getManifest();
            Method m = URLClassLoader.class.getDeclaredMethod("definePackageInternal", String.class, Manifest.class, URL.class);
            m.setAccessible(true);
            m.invoke(o,  pkgname, man, url);
        }
        // Now read the class bytes and define the class
        java.nio.ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            // Use (direct) ByteBuffer:
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, bb, cs);
        } else {
            byte[] b = res.getBytes();
            byte[] bytes = Decode(b);
            // must read certificates AFTER reading bytes.
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, bytes, 0, bytes.length, cs);
        }
    }

    protected byte[] Decode(byte[] b) {
        return b;
    }

    protected Class<?> checkInDeleteClass(Class cls, String name) throws ClassNotFoundException {
        if(null == cls) {
            return null;
        }
        String  errorClass = name;
        try {
            CustomURLClassPath ucp = (CustomURLClassPath) getPrivateProperty("ucp");
            HashSet<Class> hashSet = new HashSet<>();
            ReflectUtils.getAssociatedClass(cls, hashSet, associateFilters.toArray(new String[]{}));
            for (Class clazz:hashSet
                 ) {
                if(ucp.isRemoveURL(getClassPath(clazz, clazz.getCanonicalName()))) {
                    errorClass = clazz.getCanonicalName();
                    throw new ErrorVersionException();
                }
            }

            return cls;
        }
        catch (Exception ex) {
            throw new ClassNotFoundException(String.format("findClass: %s error", errorClass), ex);
        }

    }

    protected URL getClassPath(Class cls, String name) {
        if(loadedClass.containsKey(name)) {
            return loadedClass.get(name);
        }
        CodeSource source = cls.getProtectionDomain().getCodeSource();
        return null == source ? null : source.getLocation();
    }


    protected Object getPrivateProperty(String name) throws Exception {
        Field f = URLClassLoader.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(this);
    }

    @SuppressWarnings("SameParameterValue")
    protected void setPrivateProperty(String name, Object o) throws Exception{
        Field f = URLClassLoader.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(this, o);
    }

    protected URL[] mergeURLs(URL[] urLs, Map<URL, URL> replace, List<URL> add) {

        Set<URL> newURLs = new HashSet<>();
        Optional.ofNullable(add).ifPresent(newURLs::addAll);
       if(null == replace) {
           newURLs.addAll(Arrays.asList(urLs));
       }
       else {
           for (URL url:urLs
           ) {
               newURLs.add(replace.getOrDefault(url, url));
           }
       }

        return newURLs.toArray(new URL[]{});
    }

}

