package com.xm.sanvanfo.common.holdloader.classloader.urlloader;

import sun.misc.URLClassPath;
import sun.net.util.URLUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

@SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
public class CustomURLClassPath extends URLClassPath {

    private final HashSet<URL> removeMap = new HashSet<>();

    public CustomURLClassPath(URL[] urls) {
        super(urls);
    }


    public synchronized void addURL(URL var1) {
        Boolean removed = false;
        try {
            boolean closed = (boolean) getPrivateProperty("closed");
            if (!closed) {
                Stack<URL> urls ;
                synchronized (urls = (Stack) getPrivateProperty("urls")) {
                    ArrayList<URL> path = (ArrayList) getPrivateProperty("path");
                    if (var1 != null && !path.contains(var1)) {
                        urls.add(0, var1);
                        path.add(var1);
                        if (getPrivateProperty("lookupCacheURLs") != null) {
                            Method m = URLClassPath.class.getDeclaredMethod("disableAllLookupCaches");
                            m.setAccessible(true);
                            m.invoke(null);
                        }
                        synchronized (removeMap) {
                            if (removeMap.contains(var1)) {
                                throw new ErrorVersionException("has removed class,please renew loader");
                            }
                            else {
                                for (URL r : removeMap
                                ) {
                                    if (r.getFile().startsWith(var1.getFile())) {
                                        throw new ErrorVersionException("has removed class,please renew loader");
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void deleteURL(URL var1) {
        try {
            boolean closed = (boolean) getPrivateProperty("closed");
            if (!closed) {
                Stack<URL> urls;
                synchronized (urls  = (Stack) getPrivateProperty("urls")) {
                    urls.remove(var1);
                    ArrayList<URL> path = (ArrayList) getPrivateProperty("path");
                    path.remove(var1);
                    String var3 = URLUtil.urlNoFragString(var1);
                    HashMap hmap = (HashMap) getPrivateProperty("lmap");
                    if (hmap.containsKey(var3)) {
                        Object loader = hmap.get(var3);
                        Class cls = loader.getClass();
                        Method m = cls.getMethod("close");
                        m.setAccessible(true);
                        m.invoke(loader);
                        hmap.remove(var3);
                    }
                    if (getPrivateProperty("lookupCacheURLs") != null) {
                        Method m = URLClassPath.class.getDeclaredMethod("disableAllLookupCaches");
                        m.setAccessible(true);
                        m.invoke(null);
                    }
                    synchronized (removeMap) {
                        removeMap.add(var1);
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isRemoveURL(URL var1) throws Exception {
        if(null == var1) {
            return false;
        }
        boolean closed = (boolean)getPrivateProperty("closed");
        if(!closed) {
            synchronized (removeMap) {
                if (removeMap.contains(var1)) {
                    return true;
                }
                for (URL r : removeMap
                ) {
                    if(wrap(var1.toString()).startsWith(wrap(r.toString()))) {
                        //Judge whether the parent directory is deleted but the child directory is added in the existing address
                        ArrayList<URL> path = (ArrayList) getPrivateProperty("path");
                        for (URL url:path
                        ) {
                            if(wrap(var1.toString()).startsWith(wrap(url.toString()))) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    protected String wrap(String path) {
        boolean isFile = false;
        int last = path.lastIndexOf("/");
        if(last > 0) {
            String file = path.substring(last);
            if(file.indexOf(".") > 0) {
                isFile = true;
            }
        }
        if(isFile) {
            return path + "?";
        }
        if(!path.endsWith("/")) {
            return path + "/";
        }
        return path;
    }

    protected Object getPrivateProperty(String name) throws Exception {
        Field f = URLClassPath.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(this);
    }
}
