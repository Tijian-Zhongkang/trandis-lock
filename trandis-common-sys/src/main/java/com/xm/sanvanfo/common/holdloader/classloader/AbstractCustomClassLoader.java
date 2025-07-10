package com.xm.sanvanfo.common.holdloader.classloader;

public abstract class AbstractCustomClassLoader extends ClassLoader {


    AbstractCustomClassLoader() {
        super();
    }

    AbstractCustomClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] loadBytes = loadClassData(name);
        return defineClass(name, loadBytes, 0, loadBytes.length);
    }


    protected abstract byte[] loadClassData(String name) throws ClassNotFoundException;
}
