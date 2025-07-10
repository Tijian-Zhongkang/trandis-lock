package com.xm.sanvanfo.interfaces;

import java.util.List;

public interface Serializer<K, V> {

    String toKeyString(K k);
    K toKey(String str);
    K[] toKeyArray(String str);
    String toKeyArrayString(List<K> k);
    V toValue(String value);
    V toValue(byte[] value);
    V toValue(Object value);
    Object toObject(V v, Class clazz);
    byte[] toValueBytes(V v);
    String toValueString(V v);
}
