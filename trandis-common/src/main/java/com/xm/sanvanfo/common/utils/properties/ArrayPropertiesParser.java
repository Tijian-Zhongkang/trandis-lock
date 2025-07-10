package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.ObjectHolder;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class ArrayPropertiesParser extends SimplePropertiesParser {

    ArrayPropertiesParser(PropertiesManager propertiesManager) {
        super(propertiesManager);
    }


    @Override
    public boolean parseClassProperty(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                      ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                      Object parentObj, String parameter, String setProperty, String propName) throws Exception {
        ObjectHolder<String> objectHolder = new ObjectHolder<>();
        if(checkIsArrayOrMap(parameter, objectHolder)) {
            Object setObj = setObjectHolder.getObj();
            Field setField = setFieldHolder.getObj();

            String number = objectHolder.getObj();
            int iNumber = Integer.parseInt(number);
            setObj = reallocArray(setObj, iNumber + 1);
            setField.set(parentObj, setObj);
            setObjectHolder.setObj(setObj);
            transformParameter.setObj(Arrays.asList(true, iNumber));
        }
        else {
            transformParameter.setObj(Collections.singletonList(false));
        }
        return true;
    }


    @SuppressWarnings({"unchecked"})
    @Override
    public void parseLastParameter(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                   ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                   String propName) throws Exception {
        List<Object> parameters = (List)transformParameter.getObj();
        Class setClass = setClassHolder.getObj();
        Class componentType = setClass.getComponentType();
        Object setObj = setObjectHolder.getObj();
        if(parameters.get(0).equals(true)) {

            int iNumber = (int)parameters.get(1);
            Array.set(setObj, iNumber, stringToObject(properties.getProperty(propName), componentType, setFieldHolder.getObj().getAnnotations()));
        }
        else {
            super.parseLastParameter(properties, setToList, setClassHolder, setObjectHolder, setFieldHolder, transformParameter, propName);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void parseOtherParameter(Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                    ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder,
                                    ObjectHolder<Object> transformParameter) throws Exception {

        List<Object> parameters = (List)transformParameter.getObj();
        if(parameters.get(0).equals(true)) {
            Class setClass = setClassHolder.getObj();
            Class componentType = setClass.getComponentType();
            Object setObj = setObjectHolder.getObj();
            int iNumber = (int) parameters.get(1);
            Object obj = Array.get(setObj, iNumber);
            if (null == obj) {
                obj = newInstance(componentType, 0);
                Array.set(setObj, iNumber, obj);
            }

            setClassHolder.setObj(obj.getClass());
            setObjectHolder.setObj(obj);
        }

    }

    @Override
    public void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception {
        Class rawClass = getRawClass(clazz);
        Class componentType = rawClass.getComponentType();
        if(isSimpleType(componentType)) {
            List<String> arr = new ArrayList<>();
            int length = Array.getLength(obj);
            for(int i = 0; i < length; i++) {
                Object o = Array.get(obj, i);
                if(null != o) {
                    arr.add(o.toString());
                }
            }
            properties.setProperty(prefix, String.join(",", arr));
        }
        else {
            int length = Array.getLength(obj);
            for(int i = 0; i < length; i++) {
                Object o = Array.get(obj, i);
                if(null != o) {
                    propertiesManager.objToProperties(o.getClass(), o, String.format("%s[%d]", prefix, i), properties);
                }
            }
        }
    }

    private Object reallocArray(Object obj, int length) throws Exception {

        int oldLength = Array.getLength(obj);
        if(oldLength >= length) {
            return obj;
        }
        Object array =  newInstance(obj.getClass(), length);
        for(int i = 0; i < length;  i++) {
            if(i < oldLength) {
                Array.set(array, i, Array.get(obj, i));
            }
        }
        return array;
    }
}
