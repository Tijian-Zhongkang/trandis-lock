package com.xm.sanvanfo;

import com.xm.sanvanfo.common.plugin.IPlugin;

import java.util.List;

@SuppressWarnings("unused")
public interface IMonkeyPlugin extends IPlugin {

    void before(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType);

    Object after(Object thisObj, String name, List<Object> parameters, List<Class> paramTypes, List<Object> fields, List<Class> fieldTypes, Class returnType, Object returnValue);


}
