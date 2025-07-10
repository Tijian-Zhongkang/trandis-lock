package com.xm.sanvanfo.common.holdloader.classloader;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
public class EncryptCustomURLClassLoader extends CustomURLClassLoader implements IEncryptInterface {

    private String privateKey;


    public EncryptCustomURLClassLoader(String key, URL[] urls, Set<String> associateFilters, ClassLoader parent) throws Exception {
        super(urls, associateFilters, parent);
        privateKey = key;
    }

    public EncryptCustomURLClassLoader(String key, URL[] urls, Set<String> associateFilters) throws Exception {
        super(urls, associateFilters);
        privateKey = key;
    }

    public EncryptCustomURLClassLoader(String key, URL[] urls, Set<String> associateFilters, ClassLoader parent, URLStreamHandlerFactory acc) throws Exception {
        super(urls, associateFilters, parent, acc);
        privateKey = key;
    }


    @Override
    public Object clone(Map<URL, URL> replace, List<URL> add) throws CloneNotSupportedException {
        try {
            CustomURLClassLoader loader;
            URLStreamHandlerFactory acc = factory;
            URL[] newUrls = mergeURLs(this.getURLs(), replace, add);
            if (null != acc) {
                Constructor<? extends EncryptCustomURLClassLoader> constructor = this.getClass().getConstructor(String.class, URL[].class,  Set.class, ClassLoader.class, URLStreamHandlerFactory.class);
                loader = constructor.newInstance(this.privateKey, newUrls, associateFilters, this.getParent(), acc);
            } else if (parent != null) {
                Constructor<? extends EncryptCustomURLClassLoader> constructor = this.getClass().getConstructor(String.class, URL[].class,  Set.class, ClassLoader.class);
                loader = constructor.newInstance(this.privateKey, newUrls, associateFilters, this.getParent());
            } else {
                Constructor<? extends EncryptCustomURLClassLoader> constructor = this.getClass().getConstructor(String.class, URL[].class,  Set.class);
                loader = constructor.newInstance(this.privateKey, newUrls, associateFilters);
            }
            return loader;
        } catch (Exception ex) {
            throw new CloneNotSupportedException(ex.getMessage());
        }
    }



    @Override
    protected byte[] Decode(byte[] b) {
        return Decode(b, this.privateKey);
    }
}
