package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.ObjectHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public interface PropertiesParser {

    enum ParserType {
        SimpleType,
        Array,
        Collection,
        Map
    }

    boolean parseClassProperty(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                               ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                               Object parentObj, String parameter, String setProperty, String propName) throws Exception;

    void parseLastParameter(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                            ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                            String propName) throws Exception;

    void parseOtherParameter(Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                             ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter) throws Exception;


    void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception;
}
