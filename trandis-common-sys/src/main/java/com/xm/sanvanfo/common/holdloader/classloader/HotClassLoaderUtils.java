package com.xm.sanvanfo.common.holdloader.classloader;


import lombok.Data;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.StampedLock;

@SuppressWarnings({"unused"})
public class HotClassLoaderUtils {

    private static final StampedLock lockObj = new StampedLock();
    private static final StampedLock encryptLockObject = new StampedLock();
    private static final ConcurrentHashMap<URL, Integer> loadUrls = new ConcurrentHashMap<>();
    private static CustomURLClassLoader loader;
    private static EncryptCustomURLClassLoader encryptLoader;
    private static Set<String> associateFilters = Collections.synchronizedSet(new HashSet<>());
    private static Map<URL, BundleLoader> bundleLoaderMap = new ConcurrentHashMap<>();
    private static Map<URL, StampedLock> bundleLockMap = new ConcurrentHashMap<>();


    static {
        associateFilters.add("com.xm.sanvanfo");
    }

    public static void addAssociateFilters(List<String> list) {
        associateFilters.addAll(list);
        if(null != loader) {
            loader.addAssociateFilters(list);
        }
        if(null != encryptLoader) {
            encryptLoader.addAssociateFilters(list);
        }
    }

    public static void addOrUpdateBundleJar(URL url) throws Exception {
       addOrUpdateBundleUrl(url, false, null);
    }

    public static void deleteBundleJar(URL url) throws Exception {
        deleteBundleUrl(url);
    }

    public static void addOrUpdateBundlePackage(URL url, Boolean encrypt, String privateKey) throws Exception {
        addOrUpdateBundleUrl(url, encrypt, privateKey);
    }

    public static void deleteBundlePackage(URL url) throws Exception {
        deleteBundleUrl(url);
    }

    public static void addBundleDepends(URL url, List<URL> depends) throws Exception {
        if(null == depends || depends.size() == 0) {
            return;
        }
        URL standard = standard(url);
        BundleLoader loader = getBundleLoader(url);
        if(null == loader) {
            return;
        }
        CustomURLClassLoader classLoader = loader.encrypt ? loader.encryptLoader : loader.loader;
        StampedLock lock = bundleLockMap.computeIfAbsent(standard, o->new StampedLock());
        long stamp = lock.writeLock();
        try {
            for (URL depend:depends
                 ) {
                URL urlStandard = standard(depend);
                classLoader.addCustomURL(urlStandard);
                loader.getDepends().add(urlStandard);
            }
        }
        finally {
            lock.unlockWrite(stamp);
        }
    }

    public static ClassLoader getBundleClassloader(URL url) throws Exception {
        BundleLoader loader = getBundleLoader(url);
        if(null == loader) {
            return null;
        }
        return loader.encrypt ? loader.encryptLoader : loader.loader;
    }

    public static void addOrUpdateJar(URL url) throws Exception {
        addOrUpdateUrl(url, false, null);
    }

    public static void deleteJar(URL url)  throws Exception{
        deleteUrl(url);
    }

    public static void addOrUpdateClassesPackage(URL url, Boolean encrypt, String privateKey)  throws Exception{
        addOrUpdateUrl(url, encrypt, privateKey);
    }

    public static void deleteClassesPackage(URL url) throws Exception {
        deleteUrl(url);
    }

    public static ClassLoader getClassLoader(Boolean encrypt) {
        long stamp = encrypt ? encryptLockObject.readLock() : lockObj.readLock();
        try {
            return encrypt ? encryptLoader : loader;
        }
        finally {
            if (encrypt) {
                encryptLockObject.unlockRead(stamp);
            } else {
                lockObj.unlockRead(stamp);
            }
        }
    }

    public static Class loadClass(String name, Boolean encrypt) throws ClassNotFoundException {
        long stamp = encrypt ? encryptLockObject.readLock() : lockObj.readLock();
        try {
            if (encrypt) {
                return encryptLoader.loadClass(name);
            } else {
                return loader.loadClass(name);
            }
        }
        finally {
            if (encrypt) {
                encryptLockObject.unlockRead(stamp);
            } else {
                lockObj.unlockRead(stamp);
            }
        }
    }

    private static BundleLoader getBundleLoader(URL url) throws Exception {
        URL standard = standard(url);
        BundleLoader loader = bundleLoaderMap.get(standard);
        if(null == loader) {
            return null;
        }
        StampedLock lock = bundleLockMap.computeIfAbsent(standard, o->new StampedLock());
        long stamp = lock.readLock();
        try {
            return loader;
        }
        finally {
            lock.unlockRead(stamp);
        }
    }

    private static void deleteBundleUrl(URL url) throws Exception {
        URL urlStandard = standard(url);
        BundleLoader bundleLoader = getBundleLoader(url);
        if(null == bundleLoader) {
            return;
        }

        if(bundleLoader.isRemoved()) {
            return;
        }
        StampedLock lock = bundleLockMap.computeIfAbsent(urlStandard, o->new StampedLock());
        long stamp = lock.writeLock();
        try {
            bundleLoader.setRemoved(true);
            CustomURLClassLoader classLoader = bundleLoader.encrypt ? bundleLoader.encryptLoader : bundleLoader.loader;
            classLoader.deleteCustomURL(urlStandard);
            for (URL depend:bundleLoader.getDepends()
                 ) {
                classLoader.deleteCustomURL(depend);
            }
            bundleLoader.getDepends().clear();
        }
        finally {
            lock.unlockWrite(stamp);
        }
    }

    private static void addOrUpdateBundleUrl(URL url, Boolean encrypt, String privateKey) throws Exception {
        BundleLoader oldLoader = getBundleLoader(url);
        URL urlStandard = standard(url);
        StampedLock lock = bundleLockMap.computeIfAbsent(urlStandard, o->new StampedLock());
        long stamp = lock.writeLock();
        try {
            BundleLoader loader = new BundleLoader();
            loader.setUrl(urlStandard);
            loader.setDepends(new ConcurrentSkipListSet<>(Comparator.comparing(URL::toString)));
            bundleLoaderMap.put(urlStandard, loader);
            loader.setRemoved(false);
            loader.setEncrypt(encrypt);
            loader.setPrivateKey(privateKey);
            if(!encrypt) {
                if (null == oldLoader || null == oldLoader.getLoader()) {
                    loader.setLoader(new CustomURLClassLoader(new URL[]{urlStandard}, associateFilters));
                }
                else {
                    loader.setLoader((CustomURLClassLoader) oldLoader.getLoader().clone(new HashMap<>(), Collections.singletonList(urlStandard)));
                }
            }
            else {
               if(null == oldLoader || null == oldLoader.getEncryptLoader()) {
                   loader.setLoader(new EncryptCustomURLClassLoader(privateKey, new URL[]{url}, associateFilters));
               }
               else {
                   loader.setEncryptLoader((EncryptCustomURLClassLoader) oldLoader.getEncryptLoader().clone(new HashMap<>(), Collections.singletonList(urlStandard)));
               }
            }
        }
        finally {
            lock.unlockWrite(stamp);
        }
    }

    private static void checkCreateClassLoader(URL url, Boolean encrypt, String privateKey) throws Exception {
        if(!encrypt) {
            if(loader != null) {
                return;
            }
            long stamp = lockObj.writeLock();
            try {
                loader = new CustomURLClassLoader(new URL[]{url}, associateFilters);
            }
            finally {
                lockObj.unlockWrite(stamp);
            }
        }
        else {
            if(encryptLoader != null) {
                return;
            }
            long stamp = encryptLockObject.writeLock();
            try {
                loader = new EncryptCustomURLClassLoader(privateKey, new URL[]{url}, associateFilters);
            }
            finally {
                encryptLockObject.unlockWrite(stamp);
            }
        }
    }

    private static void addOrUpdateUrl(URL url, Boolean encrypt, String privateKey) throws Exception {
        URL urlStandard = standard(url);
        checkCreateClassLoader(urlStandard, encrypt, privateKey);
        Map<URL, URL> replace = new HashMap<>();
        List<URL> add = new ArrayList<>();
        Boolean ret = needNewLoader(urlStandard, replace, add);
        if(!ret) {
            if(!encrypt) {
                long stamp = lockObj.readLock();
                try {
                    loader.addCustomURL(urlStandard);
                } finally {
                    lockObj.unlockRead(stamp);
                }
            }
            else {
                long stamp = encryptLockObject.readLock();
                try {
                    encryptLoader.addCustomURL(urlStandard);
                }
                finally {
                    encryptLockObject.unlockRead(stamp);
                }
            }
        }
        else{
            if(!encrypt) {
                long stamp = lockObj.writeLock();
                try {
                    loader = (CustomURLClassLoader) loader.clone(replace, add);
                } finally {
                    lockObj.unlockWrite(stamp);
                }
            }
            else {
                long stamp = encryptLockObject.writeLock();
                try {
                    encryptLoader = (EncryptCustomURLClassLoader) encryptLoader.clone(replace, add);
                } finally {
                    encryptLockObject.unlockWrite(stamp);
                }
            }
        }
        loadUrls.put(urlStandard, !encrypt ? 0 : 1);
    }

    private static void deleteUrl(URL url) throws Exception {
        URL urlStandard = standard(url);
        Integer ret = loadUrls.get(urlStandard);
        if(null == ret || ret.equals(2)) {
            return;
        }
        if(ret.equals(0)) {
            long stamp = lockObj.writeLock();
            try {
                loader.deleteCustomURL(urlStandard);
            }
            finally {
                lockObj.unlockWrite(stamp);
            }
        }
        else if(ret.equals(1)) {
            long stamp = encryptLockObject.writeLock();
            try {
                encryptLoader.deleteCustomURL(urlStandard);
            }
            finally {
                encryptLockObject.unlockWrite(stamp);
            }
        }
        loadUrls.put(urlStandard, 2);
    }

    private static Boolean needNewLoader(URL url, Map<URL, URL> replace, List<URL> add) {
        Enumeration<URL> urls = loadUrls.keys();
        while (urls.hasMoreElements()) {
            URL u = urls.nextElement();
            if(wrap(u.getFile()).startsWith(wrap(url.getFile()))) {
                if(!u.sameFile(url)) {
                    if(!loadUrls.get(u).equals(2)) {
                        replace.put(u, url);
                    }
                    else {
                        add.add(url);
                    }
                }
                else {
                    add.add(url);
                }
                return true;
            }
            if(wrap(url.getFile()).startsWith(wrap(u.getFile()))) {
                if(loadUrls.get(u).equals(2)) {
                    add.add(url);
                }
                return true;
            }
        }
        return false;
    }

    private static String wrap(String path) {
        boolean isFile = false;
        int last = path.lastIndexOf("/");
        if(last > 0) {
            String file = path.substring(last);
            if(file.indexOf(".") > 0) {
                isFile = true;
            }
        }
        if(isFile) {
            return path;
        }
        if(!path.endsWith("/")) {
            return path + "/";
        }
        return path;
    }

    private static URL standard(URL url) throws MalformedURLException, URISyntaxException {
        URI uri = url.toURI();
        String host = uri.getHost();
        if(null == host) {
            host = "";
        }
        return new URL(url.getProtocol().toLowerCase(), host.toLowerCase(), url.getPort(), wrap(url.getFile()));
    }

    @Data
    private static class BundleLoader {
        private URL url;
        private Boolean encrypt;
        private String privateKey;
        private CustomURLClassLoader loader;
        private EncryptCustomURLClassLoader encryptLoader;
        private Set<URL> depends;
        private boolean removed;
    }
}
