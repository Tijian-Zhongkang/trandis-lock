package com.xm.sanvanfo.common.utils;

public class ObjectUtils {

    @SuppressWarnings("unchecked")
    public static int compare(Object param1, Object param2) {
        if(null == param1 && null == param2) {
            return 0;
        }
        if(null == param1) {
            return -1;
        }
        if(null == param2) {
            return 1;
        }
        if(Comparable.class.isAssignableFrom(param1.getClass())) {
            return ((Comparable)param1).compareTo(param2);
        }
        throw new RuntimeException("类型必须是Comparable");
    }
}
