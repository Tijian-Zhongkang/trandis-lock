package com.xm.sanvanfo.trandiscore.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializerTypeUtils {

    private static final Map<Byte, String> map = new ConcurrentHashMap<>();

    public static void registerSerializerType(byte code, String name) {
        try {
            SerializerType.getByCode(code);
        }
        catch (IllegalArgumentException ignore) {
            map.put(code, name);
        }
    }

    public static String getByCode(byte code) {
        try {
            return SerializerType.getByCode(code).name();
        }
        catch (IllegalArgumentException ignore){
            return map.get(code);
        }
    }

}
