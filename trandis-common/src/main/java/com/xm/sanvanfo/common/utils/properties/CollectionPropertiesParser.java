package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.CollectionUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class CollectionPropertiesParser extends AbstractPropertiesParser {

    CollectionPropertiesParser(PropertiesManager propertiesManager) {
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
            List collection;
            if(checkToSetTemp(setObj, setToList)) {
                Set set = (Set)setObj;
                collection = setToList.get(set);
            }
            else if(List.class.isAssignableFrom(setObj.getClass())) {
                collection = (List)setObj;
            }
            else {
                throw new Exception("you must use list or set for collection");
            }
            reallocList(collection, iNumber + 1);
            Type componentType = getActualTypeArgument0(setField, 0);
            transformParameter.setObj(Arrays.asList(true, collection, componentType, iNumber));
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
        if(parameters.get(0).equals(true)) {
            List collection = (List)parameters.get(1);
            Type componentType = (Type) parameters.get(2);
            int iNumber = (int)parameters.get(3);
            Class elemType = Object.class;
            Class componentRawType = null;
            if (ParameterizedType.class.isAssignableFrom(componentType.getClass())) {
                elemType = (Class) ((ParameterizedType) componentType).getActualTypeArguments()[0];
                componentRawType = (Class) ((ParameterizedType) componentType).getRawType();
            }
            if (componentRawType != null && Collection.class.isAssignableFrom(componentRawType)) {
                collection.set(iNumber, stringToCollection(properties.getProperty(propName), componentRawType, elemType, setFieldHolder.getObj().getAnnotations()));
            } else {
                Asserts.isTrue(componentType instanceof Class);
                collection.set(iNumber, stringToObject(properties.getProperty(propName), (Class) componentType, setFieldHolder.getObj().getAnnotations()));
            }
        }
        else {
            if(StringUtils.isEmpty(properties.getProperty(propName))) {
                return;
            }
            Field setField = setFieldHolder.getObj();
            Class setFieldType = null;
            if(setField != null) {
                setFieldType = setField.getType();
            }

            Asserts.noNull(setField);
            Class componentType = getActualTypeArgument(setField, 0);
            Object setObj = setClassHolder.getObj();
            setField.set(setObj,  stringToCollection(properties.getProperty(propName), setFieldType, componentType, setField.getAnnotations()));
        }

    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void parseOtherParameter(Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                    ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder,
                                    ObjectHolder<Object> transformParameter) throws Exception {

        List<Object> parameters = (List)transformParameter.getObj();
        if(parameters.get(0).equals(true)) {
            List collection = (List)parameters.get(1);
            Type componentType = (Type) parameters.get(2);
            int iNumber = (int)parameters.get(3);
            Object obj = collection.get(iNumber);
            if (null == obj) {
                obj = newInstance((Class) componentType, 0);
                collection.set(iNumber, obj);
            }

            setClassHolder.setObj(obj.getClass());
            setObjectHolder.setObj(obj);
        }

    }

    @Override
    public void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception {
        if(ParameterizedType.class.isAssignableFrom(clazz.getClass())) {
            Type componentType = ((ParameterizedType)clazz).getActualTypeArguments()[0];
            Collection collection = (Collection)obj;
            if(isSimpleType(getRawClass(componentType))) {
                List<String> arr = new ArrayList<>();
                for (Object component : collection) {
                    arr.add(component.toString());
                }
                properties.setProperty(prefix, String.join(",", arr));

            }
            else {
                int i = 0;
                for (Object component : collection) {
                    propertiesManager.objToProperties(componentType, component, String.format("%s[%d]", prefix, i), properties);
                    i++;
                }
            }
        }
        else {
            throw new Exception("collection must set component type");
        }
    }

    @SuppressWarnings({"unchecked"})
    Object stringToCollection(String str, Class setFieldType, Class componentType, Annotation[] annotations) throws Exception {
        if(Collection.class.isAssignableFrom(setFieldType)) {
            String[] setValues = str.split(",");

            Collection collection = CollectionUtils.createCollection(setFieldType, componentType, setValues.length);
            for (String setValue:setValues
            ) {
                Object o = stringToObject(setValue, componentType, annotations);
                collection.add(o);
            }
            return collection;
        }
        throw new Exception("class is not collection");
    }

    @SuppressWarnings({"unchecked"})
    private void reallocList(List collection, int length) {
        for(int i = collection.size(); i < length; i++) {
            collection.add(null);
        }
    }
}
