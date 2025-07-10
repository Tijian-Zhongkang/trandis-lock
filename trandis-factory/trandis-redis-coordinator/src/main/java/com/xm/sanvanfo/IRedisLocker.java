package com.xm.sanvanfo;

import com.xm.sanvanfo.trandiscore.serializer.Serializer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface IRedisLocker extends IContainNoScript {

    <T> T execScript(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception;

    <T> LockerConvertFuture<T> execScriptAsync(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception;

    void addNode(String NodeKey, String nodeId);


    void deleteNode(String NodeKey, String NodeId);

    List<String> getAllNode(String nodeKey);

    void logLockInfo();

    Serializer getSerializer();


    default String quote(String key) {
        if(key.contains("%")) {
            key = key.replaceAll("%", "%25");
        }
        if(key.contains("{")) {
            key = key.replaceAll("\\{", "%7B");
        }
        if(key.contains("}")) {
            key = key.replaceAll("}", "%7D");
        }
        return key;
    }

    default String quote(String key, String space) {
        return String.format("%s-{%s}",  quote(key), space);
    }
}
