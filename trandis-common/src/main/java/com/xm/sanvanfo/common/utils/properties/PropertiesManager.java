package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.ObjectHolder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PropertiesManager {

    private Map<PropertiesParser.ParserType, PropertiesParser> parserMap = new ConcurrentHashMap<>();

    public PropertiesManager() {
        parserMap.put(PropertiesParser.ParserType.SimpleType, new SimplePropertiesParser(this));
        parserMap.put(PropertiesParser.ParserType.Array, new ArrayPropertiesParser(this));
        parserMap.put(PropertiesParser.ParserType.Collection, new CollectionPropertiesParser(this));
        parserMap.put(PropertiesParser.ParserType.Map, new MapPropertiesParser(this));
    }

    public void parsePropertiesParameter(Properties properties, Map<Set, ArrayList> setToList,
                                         ObjectHolder<Class> setClassHolder, ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder,
                                         Object parentObj, String parameter, String setProperty, String propName) throws Exception {

        Class setClass = setClassHolder.getObj();
        PropertiesParser parser = getTypeParser(setClass);
        ObjectHolder<Object> holder = new ObjectHolder<>();
        boolean ret = parser.parseClassProperty(properties, setToList, setClassHolder, setObjectHolder, setFieldHolder, holder,
                parentObj, parameter, setProperty, propName);
        if(!ret) {
            return;
        }
        if(setProperty.equals(parameter)) {
            parser.parseLastParameter(properties, setToList, setClassHolder, setObjectHolder, setFieldHolder, holder, propName);
        }
        else {
            parser.parseOtherParameter(setToList, setClassHolder, setObjectHolder, setFieldHolder, holder);
        }
    }

    public void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception {
        Class rawClass;
        if(ParameterizedType.class.isAssignableFrom(clazz.getClass())) {
            rawClass = (Class)((ParameterizedType)clazz).getRawType();
        }
        else {
            rawClass = (Class)clazz;
        }
        PropertiesParser parser = getTypeParser(rawClass);
        parser.objToProperties(clazz, obj, prefix, properties);
    }

    private PropertiesParser getTypeParser(Class fieldClass) {
        if(Collection.class.isAssignableFrom(fieldClass)) {
            return parserMap.get(PropertiesParser.ParserType.Collection);
        }
        if(Map.class.isAssignableFrom(fieldClass)) {
            return parserMap.get(PropertiesParser.ParserType.Map);
        }
        if(fieldClass.isArray()) {
            return parserMap.get(PropertiesParser.ParserType.Array);
        }
        return parserMap.get(PropertiesParser.ParserType.SimpleType);
    }

}
