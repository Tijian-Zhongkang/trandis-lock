package com.xm.sanvanfo.common.utils.properties;

import com.google.common.collect.Sets;
import com.xm.sanvanfo.common.annotation.PropertiesItem;
import com.xm.sanvanfo.common.annotation.PropertyDateFormat;
import com.xm.sanvanfo.common.utils.CollectionUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPropertiesParser implements PropertiesParser {

    private final Map<Class, Class> primitiveMap = new ConcurrentHashMap<>();
    private final Set<Class> simpleSet = Collections.synchronizedSet(Sets.newHashSet()) ;
    final PropertiesManager propertiesManager;

    AbstractPropertiesParser(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(short.class, Short.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(double.class, Double.class);
        primitiveMap.put(boolean.class, Boolean.class);
        simpleSet.add(Integer.class);
        simpleSet.add(Long.class);
        simpleSet.add(Character.class);
        simpleSet.add(Byte.class);
        simpleSet.add(Short.class);
        simpleSet.add(Float.class);
        simpleSet.add(Double.class);
        simpleSet.add(Boolean.class);
        simpleSet.add(String.class);
        simpleSet.add(BigDecimal.class);
        simpleSet.add(BigInteger.class);
        simpleSet.add(Date.class);
    }

    @SuppressWarnings({"unchecked"})
    Object stringToObject(String str, Class setFieldType, Annotation[] annotation) throws Exception {
        if(setFieldType.isArray()) {
            String[] setValues = str.split(",");
            Class componentType = setFieldType.getComponentType();
            Object array =  Array.newInstance(componentType, setValues.length);

            for (int i = 0; i < setValues.length; i++
            ) {
                Object o = stringToObject(setValues[i], componentType, annotation);
                Array.set(array, i, o);
            }
            return array;
        }

        if (setFieldType.isPrimitive()) {
            setFieldType = primitiveMap.get(setFieldType);
        }
        if (setFieldType.equals(String.class)) {
            return str;
        }
        else if(setFieldType.equals(BigInteger.class)) {
            return new BigInteger(str);
        }
        else if(setFieldType.equals(BigDecimal.class)) {
            return new BigDecimal(str);
        }
        else if(setFieldType.equals(Date.class)) {
            PropertyDateFormat format = getAnnotation(annotation, PropertyDateFormat.class);
            String dateFormat = null == format ? "yyyy-MM-dd HH:mm:ss" : format.pattern();
            String langTag = null == format ? "en-US" : format.localeLangTag();
            String zone = null == format ? "Asia/Shanghai" : format.zone();
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.forLanguageTag(langTag));
            sdf.setTimeZone(TimeZone.getTimeZone(zone));
            return sdf.parse(str);
        }
        else {
            String methodName = "parse" + setFieldType.getSimpleName();
            if (setFieldType.equals(Integer.class)) {
                methodName = "parseInt";
            }
            Method method = setFieldType.getMethod(methodName, String.class);
            return method.invoke(null, str);
        }

    }

    private <U extends Annotation> U getAnnotation(Annotation[] annotation, Class<U> propertyDateFormatClass) {
        if(null == annotation || annotation.length == 0) {
            return null;
        }
        for (Annotation an:annotation
        ) {
            if(an.getClass().equals(propertyDateFormatClass)) {
                return propertyDateFormatClass.cast(an);
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    protected Object newInstance(Class setClass, int length) throws Exception {
        if(setClass.isArray()) {
            Class componentType = setClass.getComponentType();
            if(length < 1) {
                length = 1;
            }
            return Array.newInstance(componentType, length);
        }
        else if(Collection.class.isAssignableFrom(setClass)) {
            return CollectionUtils.createCollection(setClass, null, length);
        }
        else if(Map.class.isAssignableFrom(setClass)) {
            return new LinkedHashMap(length);
        }
        Constructor constructor = setClass.getConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    Class getActualTypeArgument(Field setField, int index) {
        return (Class)getActualTypeArgument0(setField, index);
    }

    Type getActualTypeArgument0(Field setField, int index) {
        Type componentType = Object.class;

        Type type = setField.getGenericType();
        if(ParameterizedType.class.isAssignableFrom(type.getClass())) {
            componentType =  ((ParameterizedType)type).getActualTypeArguments()[index];
        }
        return componentType;
    }

    boolean checkIsArrayOrMap(String parameter, ObjectHolder<String> holder) throws Exception {
        Pattern pattern = Pattern.compile("\\[(.+)]$");
        Matcher matcher = pattern.matcher(parameter);
        int count = 0;
        while (matcher.find()) {
            if (holder != null) {
                holder.setObj(matcher.group(1).replaceAll("[\"']", ""));
            }
            count++;
        }
        if(count > 1) {
            throw new Exception("Multidimensional collection is not supported");
        }
        return count > 0;
    }

    boolean checkToSetTemp(Object o, Map<Set, ArrayList> mapTemp) {

        if(Set.class.isAssignableFrom(o.getClass())) {
            Set set = (Set)o;
            if(!mapTemp.containsKey(set)) {
                mapTemp.put((Set)o, new ArrayList());
            }
            return true;
        }
        return false;
    }

    boolean isSimpleType(Class clazz) {
        if(clazz.isPrimitive()) {
            return true;
        }
        return simpleSet.contains(clazz);
    }

    boolean isSimpleType(Object o) {
        return o.getClass().isPrimitive() || simpleSet.contains(o.getClass());
    }

    boolean isSupported(Object obj) {
        if(isSimpleType(obj)) {
            return true;
        }
        Class clazz = obj.getClass();
        return !clazz.getCanonicalName().startsWith("java.");
    }

    Class getRawClass(Type type) {
        Class rawClass;
        if(ParameterizedType.class.isAssignableFrom(type.getClass())) {
            rawClass = (Class)((ParameterizedType)type).getRawType();
        }
        else {
            rawClass = (Class)type;
        }
        return rawClass;
    }

    Field getFieldByAnnotation(Class clazz, String fieldName) {
        for (;
             clazz != null && !clazz.equals(Object.class);
             clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f:fields
            ) {
                PropertiesItem item = f.getAnnotation(PropertiesItem.class);
                if(item != null && item.name().equals(fieldName)) {
                    return f;
                }
            }
        }
        return null;
    }

    String getPropertyKey(String prefix, Field field, ObjectHolder<Boolean> holder) {
        holder.setObj(true);
        PropertiesItem item = field.getAnnotation(PropertiesItem.class);
        if(null != item) {
            if(item.ignore()) {
                holder.setObj(false);
                return null;
            }
            if(StringUtils.isNotEmpty(item.name())) {
                return StringUtils.isEmpty(prefix) ? item.name() : prefix + "." + item.name();
            }
        }
        return StringUtils.isEmpty(prefix) ? field.getName() : prefix + "." + field.getName();
    }

}
