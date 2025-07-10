package com.xm.sanvanfo.common.hashmap;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public interface ArrayObjectConverter<E> {

    default Object convertToObject(E e)  throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException  {
        return convertToArrayObject(e);
    }

    default Object convertToArrayObject(Object e) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class component = e.getClass().getComponentType();
        if(component != null) {
            if (component.isPrimitive()) {
                return primitiveArrayToObject(e, component);
            }
            Object[] objs = (Object[])e;
            List<Object> list = new ArrayList<>();
            for (Object obj:objs
            ) {
                list.add(convertToArrayObject(obj));
            }
            return list;
        }
        else {
            return e;
        }
    }


    default Object primitiveArrayToObject(Object e, Class component) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String name = component.getName();
        StringBuilder sb = new StringBuilder();
        String namePart1 = name.substring(0, 1).toUpperCase();
        String namePart2 = name.substring(1);
        sb.append(namePart1).append(namePart2);
        Class<?> clazz = Class.forName("java.nio." + sb.toString() + "Buffer");
        return clazz.getMethod("wrap", e.getClass()).invoke(null, e);
    }

}
