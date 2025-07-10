package com.xm.sanvanfo.common;

import java.io.IOException;
import java.lang.reflect.Type;

public interface ISerializerFactory {

    <T> String serialize(T t, Type clazz, String serializerType) throws IOException;
    <T> byte[] serialize(T t, Type clazz, String serializerType, String encode) throws IOException;
    <T> T deserialize(String str, Type clazz, String serializerType) throws IOException;
    <T> T deserialize(byte[] buf, Type clazz, String serializerType, String encode) throws IOException;
    <T1,T2> T2 convert(T1 t1, Class<T2> clazz) throws IOException;
}
