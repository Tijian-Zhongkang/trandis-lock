package com.xm.sanvanfo.trandiscore.serializer;

import java.nio.charset.StandardCharsets;

public interface Serializer {
    /**
     * Encode object to byte[].
     *
     * @param <T> the type parameter
     * @param t   the t
     * @return the byte [ ]
     */
    <T> byte[] serialize(T t);

    /**
     * Decode t from byte[].
     *
     * @param <T>   the type parameter
     * @param bytes the bytes
     * @return the t
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);


    /**
     * Encode object to String.
     *
     * @param <T> the type parameter
     * @param t   the t
     * @return the String
     */
     default <T> String serializeString(T t) {
         byte[] bytes = serialize(t);
         return new String(bytes, StandardCharsets.UTF_8);
     }

     default <T> T deserializeFormString(String str, Class<T> clazz) {
         byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
         return deserialize(bytes, clazz);
     }
}
