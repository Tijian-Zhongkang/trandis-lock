package com.xm.sanvanfo.common.utils.properties;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ObjectHolder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class MapPropertiesParser extends CollectionPropertiesParser {

    MapPropertiesParser(PropertiesManager propertiesManager) {
        super(propertiesManager);
    }

    @Override
    public boolean parseClassProperty(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                      ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                      Object parentObj, String parameter, String setProperty, String propName) throws Exception {
        String mapKey = parameter;
        ObjectHolder<String> objectHolder = new ObjectHolder<>();
        if(checkIsArrayOrMap(parameter, objectHolder)) {
            mapKey = objectHolder.getObj();
        }
        Field setField = setFieldHolder.getObj();
        Type componentType = getActualTypeArgument0(setField, 1);
        transformParameter.setObj(Arrays.asList(mapKey, componentType));
        return true;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void parseLastParameter(Properties properties, Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                   ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder, ObjectHolder<Object> transformParameter,
                                   String propName) throws Exception {

        Class elemType = Object.class;
        Class componentRawType = null;
        Map map = (Map)setObjectHolder.getObj();
        List parameters = (List)transformParameter.getObj();
        String mapKey = parameters.get(0).toString();
        Type componentType = (Type)parameters.get(1);
        if(ParameterizedType.class.isAssignableFrom(componentType.getClass())) {
            elemType = (Class)((ParameterizedType)componentType).getActualTypeArguments()[0];
            componentRawType = (Class)((ParameterizedType)componentType).getRawType();
        }
        if(componentRawType != null && Collection.class.isAssignableFrom(componentRawType)) {
            map.put(mapKey, stringToCollection(properties.getProperty(propName), componentRawType, elemType, setFieldHolder.getObj().getAnnotations()));
        }
        else {
            Asserts.isTrue(componentType instanceof Class);
            map.put(mapKey, stringToObject(properties.getProperty(propName), (Class) componentType, setFieldHolder.getObj().getAnnotations()));
        }

    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void parseOtherParameter(Map<Set, ArrayList> setToList, ObjectHolder<Class> setClassHolder,
                                    ObjectHolder<Object> setObjectHolder, ObjectHolder<Field> setFieldHolder,
                                    ObjectHolder<Object> transformParameter) throws Exception {

        Object obj;
        Map map = (Map)setObjectHolder.getObj();
        List parameters = (List)transformParameter.getObj();
        String mapKey = parameters.get(0).toString();
        Type componentType = (Type)parameters.get(1);
        if(map.containsKey(mapKey)) {
            obj = map.get(mapKey);
        }
        else {
            obj = newInstance((Class)componentType, 0);
            map.put(mapKey, obj);
        }
        setClassHolder.setObj(obj.getClass());
        setObjectHolder.setObj(obj);

    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void objToProperties(Type clazz, Object obj, String prefix, Properties properties) throws Exception {
        if(ParameterizedType.class.isAssignableFrom(clazz.getClass())) {
            Type componentType = ((ParameterizedType)clazz).getActualTypeArguments()[1];
            Map<String, ?>  map = (Map)obj;
            if(isSimpleType(getRawClass(componentType))) {
                for (Map.Entry<String, ?> entry: map.entrySet()
                ) {
                    properties.setProperty(String.format("%s[\"%s\"]", prefix, entry.getKey()), entry.getValue().toString());
                }
            }
            else {
                for(Map.Entry<String, ?> entry: map.entrySet()
                ) {
                    propertiesManager.objToProperties(componentType, entry.getValue(), String.format("%s[\"%s\"]", prefix, entry.getKey()), properties);
                }
            }
        }
        else {
            throw new Exception("map must set component type");
        }
    }
}
