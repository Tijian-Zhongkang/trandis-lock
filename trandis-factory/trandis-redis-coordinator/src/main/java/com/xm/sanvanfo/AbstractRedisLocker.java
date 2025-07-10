package com.xm.sanvanfo;

import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "WeakerAccess"})
public abstract class AbstractRedisLocker<K,V> implements IRedisLocker {


    protected final com.xm.sanvanfo.interfaces.Serializer<K,V> redisSerializer;
    protected final Serializer objSerializer;
    protected final CoordinatorConfig config;

    protected AbstractRedisLocker(CoordinatorConfig config, com.xm.sanvanfo.interfaces.Serializer<K,V> redisSerializer, Serializer objSerializer) {
        this.config = config;
        this.redisSerializer = redisSerializer;
        this.objSerializer = objSerializer;
    }

    @Override
    public Serializer getSerializer() {
        return this.objSerializer;
    }

    protected Set convertToSet(Object result, Class clazz) {
        Set set = instance(clazz, LinkedHashSet.class);
        if(List.class.isAssignableFrom(result.getClass())) {
            List<V> listResult = (List)result;
            set.addAll(listResult.stream().map(redisSerializer::toValueString).collect(Collectors.toSet()));
        }
        else {
            set.add(redisSerializer.toValueString((V)result));
        }
        return set;
    }

    protected Map convertToMap(Object result, Class clazz) {
        Map map = instance(clazz, LinkedHashMap.class);
        if(List.class.isAssignableFrom(result.getClass())) {
            List<V> listResult = (List)result;
            int size = listResult.size();
            for (int i = 0; i < size; i += 2) {
                map.put(redisSerializer.toValueString(listResult.get(i)), redisSerializer.toValueString(listResult.get(i + 1)));
            }
        }
        else {
            throw new BusinessException("result list must have 2 items at least");
        }
        return map;
    }

    protected List convertToList(Object result,  Class clazz) {
        List list = instance(clazz, ArrayList.class);
        if(List.class.isAssignableFrom(result.getClass())) {
            List<V> listResult = (List)result;
            list.addAll(listResult.stream().map(this::toValueString).collect(Collectors.toList()));
        }
        else {
            list.add(this.toValueString(result));
        }
        return list;
    }

    private String toValueString(Object obj) {
        if(obj instanceof Long) {
            return ((Long)obj).toString();
        }
        if(obj instanceof Boolean) {
            return ((Boolean)obj).toString();
        }
        return redisSerializer.toValueString((V)obj);
    }

    protected  <T> T instance(Class<T> clazz, Class<T> defaultClass) {
        T t;
        try {
            if (clazz.equals(defaultClass) || clazz.isAssignableFrom(defaultClass)) {
                t = defaultClass.newInstance();
            }
            else if(Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                throw new BusinessException("class must List or can be instance");
            }
            else {
                t = clazz.newInstance();
            }
            return t;
        }
        catch (InstantiationException|IllegalAccessException ex) {
            throw new BusinessException(ex, String.format("class must be %s or can be instance, class must have no args construct",
                    defaultClass.getSimpleName()));
        }
    }

    protected <T> T convertToT(Object result, Class<T> clazz) {
        if(null == result) {
            return null;
        }
        if(null == clazz) {
            return (T)result.toString();
        }
        if(ClassUtils.isAssignable(List.class, clazz)) {
            return (T)convertToList(result, clazz);
        }
        if(ClassUtils.isAssignable(Map.class, clazz)) {
            return (T)convertToMap(result, clazz);
        }
        if(ClassUtils.isAssignable(Set.class, clazz)) {
            return (T)convertToSet(result, clazz);
        }
        if (ClassUtils.isAssignable(Boolean.class, clazz) || ClassUtils.isAssignable(Long.class, clazz)) {
            return (T)result;
        }
        if(ClassUtils.isAssignable(Integer.class, clazz)) {
            return (T)(Integer)((Long)result).intValue();
        }
        Object rawResult = result;
        if(List.class.isAssignableFrom(result.getClass())) {
            List<V> list = (List)result;
            if(list.size() > 0) {
                rawResult = list.get(0);
            }
            else {
                rawResult = null;
            }
        }
        if(clazz.equals(String.class)) {
            return (T)redisSerializer.toValueString((V)result);
        }
        String rawResultString = redisSerializer.toValueString((V)rawResult);
        return objSerializer.deserializeFormString(rawResultString, clazz);
    }
}
