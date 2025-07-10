package com.xm.sanvanfo.common.utils;

import com.xm.sanvanfo.common.utils.properties.PropertiesManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings({"unused"})
@Slf4j
public class PropertiesFileUtils {

    private static final PropertiesManager propertiesManager = new PropertiesManager();


    public static  <T> T toConfigurationClass(File path, String propertyName, Class<T> clazz) throws Exception {

        Properties properties = new Properties();
        try(InputStream stream = new FileInputStream(path)) {
            properties.load(stream);
            return toConfigurationClass(properties, propertyName, clazz);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static  <T> T toConfigurationClass( Properties properties, String propertyName, Class<T> clazz) throws Exception {
        Constructor constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object obj = constructor.newInstance();
        Set<String> propertyNames = properties.stringPropertyNames();
        //because treeset linkhashset will sort so collection's number will be disturbed
        Map<Set, ArrayList> mapTemp = new HashMap<>();
        for (String proName:propertyNames
        ) {

            if(StringUtils.isNotEmpty(propertyName)) {
                if(!proName.startsWith(propertyName + ".")) {
                    continue;
                }
            }
            String cutName = proName;
            if(StringUtils.isNotEmpty(propertyName)) {
                if(proName.startsWith(propertyName)) {
                    cutName = proName.substring(propertyName.length() + 1);
                }
            }
            String[] parameters = cutName.split("\\.");
            String setProperty = parameters[parameters.length - 1];
            Field setField = null;
            ObjectHolder<Class> setClassHolder = new ObjectHolder<>(clazz);
            ObjectHolder<Object> setObjectHolder = new ObjectHolder<>(obj);
            ObjectHolder<Field> setFieldHolder = new ObjectHolder<>();
            for (String parameter:parameters
            ) {
                try {

                    propertiesManager.parsePropertiesParameter(properties, mapTemp, setClassHolder, setObjectHolder, setFieldHolder,
                            null, parameter, setProperty, proName);
                }
                catch (Exception ex) {
                    log.warn(String.format("property %s error, error message:%s", proName, ex.getMessage()));
                }
            }

        }
        for (Map.Entry<Set, ArrayList> entry:mapTemp.entrySet()
        ) {
            entry.getKey().addAll(entry.getValue());
        }
        mapTemp.clear();
        return  clazz.cast(obj);
    }

    public static Properties toProperties(Object obj, String prefix) throws Exception {
        if(null == obj) {
            return null;
        }
        Class clazz = obj.getClass();
        if(clazz.getCanonicalName().startsWith("java.")) {
            throw new Exception("obj is system type");
        }

        Properties properties = new Properties();
        propertiesManager.objToProperties(clazz, obj, prefix, properties);
        return properties;
    }
}
