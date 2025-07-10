package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.common.utils.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class SimplePropertiesParser extends AbstractPropertiesParser {

    SimplePropertiesParser(PropertiesManager propertiesManager) {
        super(propertiesManager);
    }

    @Override
    public boolean parseClassProperty(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                      ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                      Object parentObj, String parameter, String setProperty, String propName) throws Exception {
        Class setClass = setClassHolder.getObj();
        String fieldName = parameter;
        boolean arrayOrMap = false;
        if(checkIsArrayOrMap(parameter, null)) {
            fieldName = parameter.replaceAll("\\[.+]", "");
            arrayOrMap = true;
        }

        Field setField = ReflectUtils.getField(setClass, fieldName);
        if(setField == null) {
            setField = getFieldByAnnotation(setClass, fieldName);
        }
        Asserts.noNull(setField);
        setClass = setField.getType();
        setField.setAccessible(true);
        setClassHolder.setObj(setClass);
        setFieldHolder.setObj(setField);
        if(arrayOrMap) {
            parentObj = setObjectHolder.getObj();
            parseOtherParameter(setToList, setClassHolder, setObjectHolder, setFieldHolder, transformParameter);
            propertiesManager.parsePropertiesParameter(properties, setToList, setClassHolder, setObjectHolder,
                    setFieldHolder, parentObj, parameter, setProperty, propName);
        }
        return !arrayOrMap;
    }



    @Override
    public void parseLastParameter(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                   ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                   String propName) throws Exception {

        if(StringUtils.isEmpty(properties.getProperty(propName))) {
            return;
        }
        Field setField = setFieldHolder.getObj();
        Object setObject = setObjectHolder.getObj();
        Class setFieldType = null;
        if(setField != null) {
            setFieldType = setField.getType();
        }
        Asserts.noNull(setFieldType);
        Object setObj = stringToObject(properties.getProperty(propName), setFieldType, setField.getAnnotations());
        setField.set(setObject, setObj);
    }


    @Override
    public void parseOtherParameter(Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                    ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder,
                                    ObjectHolder<Object> transformParameter) throws Exception {

        Field setField = setFieldHolder.getObj();
        Object setObj = setObjectHolder.getObj();
        Class setClass = setClassHolder.getObj();
        Object objNew = setField.get(setObj);
        if(null == objNew) {
            objNew = newInstance(setClass, 0);
            checkToSetTemp(objNew, setToList);
            setField.set(setObj, objNew);
        }
        setObjectHolder.setObj(objNew);
    }

    @Override
    public void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception {
        if(!isSupported(obj)) {
            log.warn(String.format("type %s is not supported", obj.getClass()));
            return;
        }

        List<Field> fields = ReflectUtils.getFields(getRawClass(clazz));
        for (Field field:fields
        ) {
            if(!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if(null == value) {
                    continue;
                }
                ObjectHolder<Boolean> holder = new ObjectHolder<>();
                String key = getPropertyKey(prefix, field, holder);
                if(holder.getObj().equals(false)) {
                    continue;
                }
                if(isSimpleType(value)) {
                    properties.setProperty(key, value.toString());
                }
                else {
                    propertiesManager.objToProperties(field.getGenericType(), value, key, properties);
                }
            }
        }
    }
}
