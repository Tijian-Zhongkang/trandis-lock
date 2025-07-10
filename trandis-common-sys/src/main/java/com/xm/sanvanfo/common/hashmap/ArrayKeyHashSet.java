package com.xm.sanvanfo.common.hashmap;

import com.xm.sanvanfo.common.utils.ReflectUtils;

import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("unused")
public class ArrayKeyHashSet<E> extends HashSet<E> {

    public ArrayKeyHashSet() {
        ReflectUtils.setPropertyByFieldName(this, new ArrayKeyHashMap<>(), "map");
    }

    public ArrayKeyHashSet(Collection<? extends E> c) {
        ReflectUtils.setPropertyByFieldName(this, new ArrayKeyHashMap<>(Math.max((int) (c.size()/.75f) + 1, 16)), "map");
        addAll(c);
    }

    public ArrayKeyHashSet(int initialCapacity, float loadFactor) {
        ReflectUtils.setPropertyByFieldName(this,  new ArrayKeyHashMap<>(initialCapacity, loadFactor), "map");
    }

    public ArrayKeyHashSet(int initialCapacity) {
        ReflectUtils.setPropertyByFieldName(this,  new ArrayKeyHashMap<>(initialCapacity), "map");
    }

}
