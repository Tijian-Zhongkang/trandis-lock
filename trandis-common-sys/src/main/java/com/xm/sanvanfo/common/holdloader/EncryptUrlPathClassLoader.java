package com.xm.sanvanfo.common.holdloader;

import com.xm.sanvanfo.common.holdloader.classloader.IEncryptInterface;
import com.xm.sanvanfo.common.holdloader.classloader.UrlPathClassLoader;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;

@SuppressWarnings({"unused"})
public class EncryptUrlPathClassLoader extends UrlPathClassLoader implements IEncryptInterface {

    private String privateKey;
    public EncryptUrlPathClassLoader( String pri, String path) {
        super(path);
        privateKey = pri;
    }

    public EncryptUrlPathClassLoader( String pri, String path, ClassLoader parent) {
        super(path, parent);
        privateKey = pri;
    }

    public Object clone(String path) throws CloneNotSupportedException {
        try {
            UrlPathClassLoader loader;
            String basePath;
            if(StringUtils.isNotEmpty(path)) {
                basePath = path;
            }
            else {
                basePath = this.basePath;
            }
            if (null != parent) {
                Constructor<? extends EncryptUrlPathClassLoader> constructor = this.getClass().getConstructor(String.class, String.class, ClassLoader.class);
                loader = constructor.newInstance(this.privateKey, basePath, this.parent);
            } else {
                Constructor<? extends EncryptUrlPathClassLoader> constructor = this.getClass().getConstructor( String.class, String.class);
                loader = constructor.newInstance( this.privateKey, basePath);
            }
            return loader;
        }
        catch (Exception ex) {
            throw new CloneNotSupportedException(ex.getMessage());
        }
    }


    @Override
    protected byte[] Decode(byte[] body) {
        return Decode(body, this.privateKey);
    }

}
