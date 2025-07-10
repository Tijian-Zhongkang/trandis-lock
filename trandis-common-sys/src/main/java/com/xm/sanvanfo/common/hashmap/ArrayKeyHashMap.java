package com.xm.sanvanfo.common.hashmap;

import com.xm.sanvanfo.common.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings({"rawtypes","unchecked", "WeakerAccess","unused"}) // for cast to Comparable
public class ArrayKeyHashMap<K, V> extends HashMap<K, V> implements ArrayObjectConverter<K> {

    public ArrayKeyHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ArrayKeyHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ArrayKeyHashMap() {
        super();
    }

    public ArrayKeyHashMap(Map<? extends K, ? extends V> m) {
        Object factor = ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "DEFAULT_LOAD_FACTOR");
        ReflectUtils.setPropertyByFieldName(this, factor, "loadFactor");
        putMapEntriesEx(m, false);
    }

    @Override
    public V get(Object key) {
        Map.Entry<K,V> e;
        try {

            Map.Entry<K, V> entry = getNodeEx(hashEx(key), key);
            return (e = entry) == null ? null : e.getValue();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            Map.Entry<K, V> entry = getNodeEx(hashEx(key), key);
            return entry != null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V put(K key, V value) {
        try {

            return putValEx(hashEx(key), key, value, false, true);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntriesEx(m, true);
    }

    @Override
    public V remove(Object key) {
        try {
            Map.Entry<K, V> e;
            e = removeNodeEx(hashEx(key), key, null, false, true);
            return e == null ? null : e.getValue();

        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Map.Entry<K,V> e;
        try {
            Map.Entry<K, V> entry = getNodeEx( hashEx(key), key);
            return (e = entry) == null ? defaultValue : e.getValue();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {

        try {
            return putValEx(hashEx(key), key, value, true, true);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        try {
            Map.Entry<K, V> e;
            e = removeNodeEx(hashEx(key), key, null, true, true);
            return e != null;

        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        try {
            Map.Entry<K, V> e;
            V v;
            if ((e = getNodeEx(hashEx(key), key)) != null &&
                    ((v = e.getValue()) == oldValue || (v != null && v.equals(oldValue)))) {
                e.setValue(newValue);
                Class classNode = Class.forName("java.util.HashMap$Node");
                Method method = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", classNode);
                method.setAccessible(true);
                method.invoke(this, e);
                return true;
            }
            return false;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V replace(K key, V value) {
        try {
            Map.Entry<K, V> e;
            if ((e = getNodeEx(hashEx(key), key)) != null) {
                V oldValue = e.getValue();
                e.setValue(value);
                Class classNode = Class.forName("java.util.HashMap$Node");
                Method method = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", classNode);
                method.setAccessible(true);
                method.invoke(this, e);
                return oldValue;
            }
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        try {
            int hash = hashEx(key);
            Map.Entry<K, V>[] tab;
            Map.Entry<K, V> first;
            int n, i;
            int binCount = 0;
            Map.Entry<K, V> t = null;
            Map.Entry<K, V> old = null;
            if ((int) ReflectUtils.getPropertyByFieldName(this, "size") > (int) ReflectUtils.getPropertyByFieldName(this, "threshold") ||
                    (tab = (Map.Entry<K, V>[]) ReflectUtils.getPropertyByFieldName(this, "table")) == null ||
                    (n = tab.length) == 0) {
                Method resize = ReflectUtils.getDeclareMehodByName(this.getClass(), "resize");
                resize.setAccessible(true);
                n = (tab = (Map.Entry<K, V>[])resize.invoke(this)).length;
            }
            Class node = Class.forName("java.util.HashMap$Node");
            Method afterNodeAccess = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", node);
            afterNodeAccess.setAccessible(true);
            if ((first = tab[i = (n - 1) & hash]) != null) {
                if (first.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode"))
                    old = TreeNodeUtils.getTreeNode(t = first, hash, key, this);
                else {
                    Map.Entry<K, V> e = first;
                    K k;
                    do {
                        if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                ((k = e.getKey()) == key || (key != null && key.equals(k))
                                || (key != null && convertToObject(key).equals(convertToObject(k))))) {
                            old = e;
                            break;
                        }
                        ++binCount;
                    } while ((e = (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(e, "next")) != null);
                }
                V oldValue;
                if (old != null && (oldValue = old.getValue()) != null) {
                    afterNodeAccess.invoke(this, old);
                    return oldValue;
                }
            }
            V v = mappingFunction.apply(key);
            if (v == null) {
                return null;
            } else if (old != null) {
                old.setValue(v);
                afterNodeAccess.invoke(this, old);
                return v;
            } else if (t != null)
                TreeNodeUtils.putTreeVal(t, this, tab, hash, key, v, this);
            else {
                Method newNode = ReflectUtils.getDeclareMehodByName(this.getClass(), "newNode", int.class, Object.class, Object.class, node);
                newNode.setAccessible(true);
                tab[i] = (Map.Entry<K, V>)newNode.invoke(this, hash, key, v, first);
                if (binCount >=  (int)ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "TREEIFY_THRESHOLD") - 1) {
                    Method treeifyBin = ReflectUtils.getDeclareMehodByName(this.getClass(), "treeifyBin", node, int.class);
                    treeifyBin.setAccessible(true);
                    treeifyBin.invoke(this, tab, hash);
                }
            }
            int modCount = (int)ReflectUtils.getPropertyByFieldName(this, "modCount");
            modCount++;
            ReflectUtils.setPropertyByFieldName(this, modCount, "modCount");
            int size = (int)ReflectUtils.getPropertyByFieldName(this, "size");
            size++;
            ReflectUtils.setPropertyByFieldName(this, size, "size");
            Method afterNodeInsertion = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeInsertion", boolean.class);
            afterNodeInsertion.setAccessible(true);
            afterNodeInsertion.invoke(this, true);
            return v;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        try {
            Map.Entry<K, V> e;
            V oldValue;
            int hash = hashEx(key);
            if ((e = getNodeEx(hash, key)) != null &&
                    (oldValue = e.getValue()) != null) {
                V v = remappingFunction.apply(key, oldValue);
                if (v != null) {
                    e.setValue(v);
                    Class node = Class.forName("java.util.HashMap$Node");
                    Method method = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", node);
                    method.setAccessible(true);
                    method.invoke(this, e);
                    return v;
                } else
                    removeNodeEx(hash, key, null, false, true);
            }
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        try {
            int hash = hashEx(key);
            Map.Entry<K, V>[] tab;
            Map.Entry<K, V> first;
            int n, i;
            int binCount = 0;
            Map.Entry<K, V> t = null;
            Map.Entry<K, V> old = null;
            if ((int) ReflectUtils.getPropertyByFieldName(this, "size") > (int) ReflectUtils.getPropertyByFieldName(this, "threshold") ||
                    (tab = (Map.Entry<K, V>[]) ReflectUtils.getPropertyByFieldName(this, "table")) == null ||
                    (n = tab.length) == 0) {
                Method resize = ReflectUtils.getDeclareMehodByName(this.getClass(), "resize");
                resize.setAccessible(true);
                n = (tab = (Map.Entry<K, V>[])resize.invoke(this)).length;
            }
            Class node = Class.forName("java.util.HashMap$Node");
            Method afterNodeAccess = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", node);
            afterNodeAccess.setAccessible(true);
            if ((first = tab[i = (n - 1) & hash]) != null) {
                if (first.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode")) {
                    old = TreeNodeUtils.getTreeNode(t = first, hash, key, this);
                }
                else {
                    Map.Entry<K, V> e = first;
                    K k;
                    do {
                        if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                ((k = e.getKey()) == key || (key != null && key.equals(k)) ||
                                        (key != null && convertToObject(key).equals(convertToObject(k))))) {
                            old = e;
                            break;
                        }
                        ++binCount;
                    } while ((e = (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(e, "next")) != null);
                }
            }
            V oldValue = (old == null) ? null : old.getValue();
            V v = remappingFunction.apply(key, oldValue);
            if (old != null) {
                if (v != null) {
                    old.setValue(v);
                    afterNodeAccess.invoke(this, old);
                } else
                    removeNodeEx(hash, key, null, false, true);
            } else if (v != null) {
                if (t != null)
                    TreeNodeUtils.putTreeVal(t, this, tab, hash, key, v, this);
                else {
                    Method newNode = ReflectUtils.getDeclareMehodByName(this.getClass(), "newNode", int.class, Object.class, Object.class, node);
                    newNode.setAccessible(true);
                    tab[i] = (Map.Entry<K, V>)newNode.invoke(this, hash, key, v, first);
                    if (binCount >=  (int)ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "TREEIFY_THRESHOLD") - 1) {
                        Method treeifyBin = ReflectUtils.getDeclareMehodByName(this.getClass(), "treeifyBin", node, int.class);
                        treeifyBin.setAccessible(true);
                        treeifyBin.invoke(this, tab, hash);
                    }
                }
                int modCount = (int)ReflectUtils.getPropertyByFieldName(this, "modCount");
                modCount++;
                ReflectUtils.setPropertyByFieldName(this, modCount, "modCount");
                int size = (int)ReflectUtils.getPropertyByFieldName(this, "size");
                size++;
                ReflectUtils.setPropertyByFieldName(this, size, "size");
                Method afterNodeInsertion = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeInsertion", boolean.class);
                afterNodeInsertion.setAccessible(true);
                afterNodeInsertion.invoke(this, true);
            }
            return v;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        try {
            int hash = hashEx(key);
            Map.Entry<K, V>[] tab;
            Map.Entry<K, V> first;
            int n, i;
            int binCount = 0;
            Map.Entry<K, V> t = null;
            Map.Entry<K, V> old = null;
            if ((int) ReflectUtils.getPropertyByFieldName(this, "size") > (int) ReflectUtils.getPropertyByFieldName(this, "threshold") ||
                    (tab = (Map.Entry<K, V>[]) ReflectUtils.getPropertyByFieldName(this, "table")) == null ||
                    (n = tab.length) == 0) {
                Method resize = ReflectUtils.getDeclareMehodByName(this.getClass(), "resize");
                resize.setAccessible(true);
                n = (tab = (Map.Entry<K, V>[]) resize.invoke(this)).length;
            }
            Class node = Class.forName("java.util.HashMap$Node");
            Method afterNodeAccess = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", node);
            afterNodeAccess.setAccessible(true);
            if ((first = tab[i = (n - 1) & hash]) != null) {
                if (first.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode")) {
                    old = TreeNodeUtils.getTreeNode(t = first, hash, key, this);
                }
                else {
                    Map.Entry<K, V> e = first;
                    K k;
                    do {
                        if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                ((k = e.getKey()) == key || (key != null && key.equals(k)) ||
                                        (key != null && convertToObject(key).equals(convertToObject(k))))) {
                            old = e;
                            break;
                        }
                        ++binCount;
                    } while ((e = (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(e, "next")) != null);
                }
            }
            if (old != null) {
                V v;
                if (old.getValue() != null)
                    v = remappingFunction.apply(old.getValue(), value);
                else
                    v = value;
                if (v != null) {
                    old.setValue(v);
                    afterNodeAccess.invoke(this, old);
                } else
                    removeNodeEx(hash, key, null, false, true);
                return v;
            }
            if (t != null)
                TreeNodeUtils.putTreeVal(t, this, tab, hash, key, value, this);
            else {
                Method newNode = ReflectUtils.getDeclareMehodByName(this.getClass(), "newNode", int.class, Object.class, Object.class, node);
                newNode.setAccessible(true);
                tab[i] = (Entry<K, V>)newNode.invoke(this, hash, key, value, first);
                if (binCount >=  (int)ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "TREEIFY_THRESHOLD") - 1) {
                    Method treeifyBin = ReflectUtils.getDeclareMehodByName(this.getClass(), "treeifyBin", node, int.class);
                    treeifyBin.setAccessible(true);
                    treeifyBin.invoke(this, tab, hash);
                }
            }
            int modCount = (int)ReflectUtils.getPropertyByFieldName(this, "modCount");
            modCount++;
            ReflectUtils.setPropertyByFieldName(this, modCount, "modCount");
            int size = (int)ReflectUtils.getPropertyByFieldName(this, "size");
            size++;
            ReflectUtils.setPropertyByFieldName(this, size, "size");
            Method afterNodeInsertion = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeInsertion", boolean.class);
            afterNodeInsertion.setAccessible(true);
            afterNodeInsertion.invoke(this, true);
            return value;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    final void putMapEntriesEx(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            try {
                if (ReflectUtils.getPropertyByFieldName(this, "table") == null) { // pre-size
                    float ft = ((float) s / (float) ReflectUtils.getPropertyByFieldName(this, "loadFactor")) + 1.0F;
                    int t = ((ft < (float) (int)ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "MAXIMUM_CAPACITY")) ?
                            (int) ft : (int) ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "MAXIMUM_CAPACITY"));
                    if (t > (int) ReflectUtils.getPropertyByFieldName(this, "threshold")) {
                        Method method = ReflectUtils.getDeclareMehodByName(this.getClass(), "tableSizeFor", int.class);
                        method.setAccessible(true);
                        method.invoke(this, t);
                    }
                } else if (s > (int) ReflectUtils.getPropertyByFieldName(this, "threshold")) {
                     Method method = ReflectUtils.getDeclareMehodByName(this.getClass(), "resize");
                    method.setAccessible(true);
                    method.invoke(this);
                }
                for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                    K key = e.getKey();
                    V value = e.getValue();
                    putValEx(hashEx(key), key, value, false, evict);
                }
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    final V putValEx(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        try {
            Entry<K, V>[] tab;
            Entry<K, V> p;
            int n, i;
            Method resize = ReflectUtils.getDeclareMehodByName(this.getClass(), "resize");
            resize.setAccessible(true);
            Class node = Class.forName("java.util.HashMap$Node");
            Method newNode = ReflectUtils.getDeclareMehodByName(this.getClass(), "newNode", int.class, Object.class, Object.class, node);
            newNode.setAccessible(true);
            if ((tab = (Entry<K, V>[]) ReflectUtils.getPropertyByFieldName(this, "table")) == null || (n = tab.length) == 0)
                n = (tab = (Entry<K, V>[]) resize.invoke(this)).length;
            if ((p = tab[i = (n - 1) & hash]) == null)
                tab[i] = (Entry<K, V>) newNode.invoke(this, hash, key, value, null);
            else {
                Entry<K, V> e;
                K k;
                if ((int)ReflectUtils.getPropertyByFieldName(p, "hash") == hash &&
                        ((k = (K)ReflectUtils.getPropertyByFieldName(p, "key")) == key || (key != null && key.equals(k))
                        ||  (key != null && convertToObject(key).equals(convertToObject(k)))))
                    e = p;
                else if (p.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode")) {
                    e = TreeNodeUtils.putTreeVal(p, this, tab, hash, key, value, this);
                }
                else {
                    Method treeifyBin = ReflectUtils.getDeclareMehodByName(this.getClass(), "treeifyBin", tab.getClass(), int.class);
                    treeifyBin.setAccessible(true);
                    for (int binCount = 0; ; ++binCount) {
                        if ((e = (Entry<K, V>) ReflectUtils.getPropertyByFieldName(p, "next")) == null) {
                            Method nextNode = ReflectUtils.getDeclareMehodByName(this.getClass(), "newNode", int.class, Object.class, Object.class, node);
                            nextNode.setAccessible(true);
                            Object next = nextNode.invoke(this, hash, key, value, null);
                            ReflectUtils.setPropertyByFieldName(p,next, "next");
                            if (binCount >= (int)ReflectUtils.getStaticPropertyByFiledName(this.getClass(), "TREEIFY_THRESHOLD") - 1) // -1 for 1st
                                treeifyBin.invoke(this, tab, hash);
                            break;
                        }
                        if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                ((k =(K)ReflectUtils.getPropertyByFieldName(e, "key")) == key || (key != null && key.equals(k))
                                || (key != null && convertToObject(key).equals(convertToObject(k)))))
                            break;
                        p = e;
                    }
                }
                if (e != null) { // existing mapping for key
                    V oldValue = e.getValue();
                    if (!onlyIfAbsent || oldValue == null)
                        e.setValue(value);
                    Class classNode = Class.forName("java.util.HashMap$Node");
                    Method afterNodeAccess = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeAccess", classNode);
                    afterNodeAccess.setAccessible(true);
                    afterNodeAccess.invoke(this, e);
                    return oldValue;
                }
            }
            int modCount = (int)ReflectUtils.getPropertyByFieldName(this, "modCount");
            modCount++;
            ReflectUtils.setPropertyByFieldName(this, modCount, "modCount");
            int size = (int)ReflectUtils.getPropertyByFieldName(this, "size");
            size++;
            ReflectUtils.setPropertyByFieldName(this, size, "size");
            if (size > (int)ReflectUtils.getPropertyByFieldName(this, "threshold")) {
                resize.invoke(this);
            }
            Method afterNodeInsertion = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeInsertion", boolean.class);
            afterNodeInsertion.setAccessible(true);
            afterNodeInsertion.invoke(this, evict);
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    final Entry<K,V> getNodeEx(int hash, Object key) {
        try {
            Entry<K, V>[] tab;
            Entry<K, V> first, e;
            int n;
            K k;
            if ((tab = ( Entry<K, V>[])ReflectUtils.getPropertyByFieldName(this, "table")) != null && (n = tab.length) > 0 &&
                    (first = tab[(n - 1) & hash]) != null) {
                if ((int)ReflectUtils.getPropertyByFieldName(first, "hash") == hash && // always check first node
                        ((k = (K)ReflectUtils.getPropertyByFieldName(first, "key")) == key || (key != null && key.equals(k)) ||
                                (key != null && convertToObject((K)key).equals(convertToObject(k)))))
                    return first;
                if ((e = (Entry<K, V>) ReflectUtils.getPropertyByFieldName(first, "next")) != null) {
                    if (first.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode")) {

                        return TreeNodeUtils.getTreeNode( first, hash, key, this);
                    }
                    do {
                        if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                ((k = (K)ReflectUtils.getPropertyByFieldName(e, "key")) == key || (key != null && key.equals(k)) ||
                                        (key != null && convertToObject((K)key).equals(convertToObject(k)))))
                            return e;
                    } while ((e = (Entry<K, V>) ReflectUtils.getPropertyByFieldName(e, "next")) != null);
                }
            }
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    final Map.Entry<K,V> removeNodeEx(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        try {
            Map.Entry<K, V>[] tab;
            Map.Entry<K, V> p;
            int n, index;
            if ((tab =  ( Entry<K, V>[])ReflectUtils.getPropertyByFieldName(this, "table")) != null && (n = tab.length) > 0 &&
                    (p = tab[index = (n - 1) & hash]) != null) {
                Map.Entry<K, V> node = null, e;
                K k;
                V v;
                if ((int)ReflectUtils.getPropertyByFieldName(p, "hash") == hash &&
                        ((k = p.getKey()) == key || (key != null && key.equals(k))
                        || (key != null && convertToObject((K) key).equals(convertToObject(k)))))
                    node = p;
                else if ((e = (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(p, "next")) != null) {
                    if (p.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode"))
                        node = TreeNodeUtils.getTreeNode(p, hash, key, this);
                    else {
                        do {
                            if ((int)ReflectUtils.getPropertyByFieldName(e, "hash") == hash &&
                                    ((k = e.getKey()) == key ||
                                            (key != null && key.equals(k))
                                            || (key != null && convertToObject((K) key).equals(convertToObject(k))))) {
                                node = e;
                                break;
                            }
                            p = e;
                        } while ((e =   (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(e, "next")) != null);
                    }
                }
                if (node != null && (!matchValue || (v = node.getValue()) == value ||
                        (value != null && value.equals(v)))) {
                    if (node.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode"))
                        TreeNodeUtils.removeTreeNode(node, this, tab, movable);
                    else if (node == p)
                        tab[index] =  (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(node, "next");
                    else {
                        Map.Entry<K, V> next =  (Map.Entry<K, V>)ReflectUtils.getPropertyByFieldName(node, "next");
                        ReflectUtils.setPropertyByFieldName(p, next, "next");
                    }
                    int modCount = (int) ReflectUtils.getPropertyByFieldName(this, "modCount");
                    modCount++;
                    ReflectUtils.setPropertyByFieldName(this, modCount, "modCount");
                    int size = (int) ReflectUtils.getPropertyByFieldName(this, "size");
                    size--;
                    ReflectUtils.setPropertyByFieldName(this, size, "size");
                    Class classNode = Class.forName("java.util.HashMap$Node");
                    Method afterNodeRemoval = ReflectUtils.getDeclareMehodByName(this.getClass(), "afterNodeRemoval", classNode);
                    afterNodeRemoval.setAccessible(true);
                    afterNodeRemoval.invoke(this, node);
                    return node;
                }
            }
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private int hashEx(Object key) {
        try {
            Object o = convertToObject((K)key);
            int h;
            return (key == null) ? 0 : (h = o.hashCode()) ^ (h >>> 16);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static class TreeNodeUtils {


        static <K, V> Map.Entry<K, V> root(Map.Entry<K, V> treeNode)  {
            try {
                validateTreeNode(treeNode);
                Method method = ReflectUtils.getDeclareMehodByName(treeNode.getClass(), "root");
                method.setAccessible(true);
                return (Map.Entry<K, V>)method.invoke(treeNode);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        static  <K,V> void moveRootToFront(Map.Entry<K,V>[] tab, Map.Entry<K,V> root) {
           try {
               Class treeNode = Class.forName("java.util.HashMap$TreeNode");
               Class nodeArray = Class.forName("[Ljava.util.HashMap$Node;");
               validateTreeNode(root);
               Method method = ReflectUtils.getDeclareMehodByName(root.getClass(), "moveRootToFront", nodeArray, treeNode);
               method.setAccessible(true);
               method.invoke(null, tab, root);
           }
           catch (Exception ex) {
               throw new RuntimeException(ex);
           }
        }

        static <K, V> Map.Entry<K,V> find(Map.Entry<K,V> treeNode, int h, Object k, Class<?> kc, ArrayObjectConverter<K> converter) {
            validateTreeNode(treeNode);
            try {
                Map.Entry<K, V> p = treeNode;
                do {
                    Method comparableClassFor = ReflectUtils.getDeclareMehodByName(HashMap.class, "comparableClassFor", Object.class);
                    comparableClassFor.setAccessible(true);
                    Method compareComparables = ReflectUtils.getDeclareMehodByName(HashMap.class, "compareComparables", Class.class, Object.class, Object.class);
                    compareComparables.setAccessible(true);
                    int ph, dir;
                    K pk;
                    Map.Entry<K, V> pl = left(p), pr = right(p), q;
                    if ((ph = hash(p)) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.getKey()) == k || (k != null && k.equals(pk)) ||
                            (k != null && converter.convertToObject((K)k).equals(converter.convertToObject(pk))))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                            (kc = (Class)comparableClassFor.invoke(null, k)) != null) &&
                            (dir = (int)compareComparables.invoke(null, kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = find(pr, h, k, kc, converter)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
                return null;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        static <K> int tieBreakOrder(Object a, Object b, ArrayObjectConverter<K> converter) {
            try {
                int d;
                if (a == null || b == null ||
                        (d = a.getClass().getName().
                                compareTo(b.getClass().getName())) == 0)
                    d = (System.identityHashCode(converter.convertToObject((K) a)) <= System.identityHashCode(converter.convertToObject((K) b)) ?
                            -1 : 1);
                return d;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        static <K, V> Map.Entry<K,V> putTreeVal(Map.Entry<K, V> treeNode, HashMap<K,V> map, Map.Entry<K,V>[] tab,
                                                      int h, K k, V v, ArrayObjectConverter<K> converter) {
            try {
                Class<?> kc = null;
                boolean searched = false;
                Method comparableClassFor = ReflectUtils.getDeclareMehodByName(HashMap.class, "comparableClassFor", Object.class);
                comparableClassFor.setAccessible(true);
                Method compareComparables = ReflectUtils.getDeclareMehodByName(HashMap.class, "compareComparables", Class.class, Object.class, Object.class);
                compareComparables.setAccessible(true);
                Map.Entry<K, V> root = (parent(treeNode) != null) ? root(treeNode) : treeNode;
                for (Map.Entry<K, V> p = root; ; ) {
                    int dir, ph;
                    K pk;
                    if ((ph = hash(p)) > h)
                        dir = -1;
                    else if (ph < h)
                        dir = 1;
                    else if ((pk = p.getKey()) == k || (k != null && k.equals(pk)))
                        return p;
                    else if ((kc == null &&
                            (kc = (Class)comparableClassFor.invoke(null, k)) == null) ||
                            (dir = (int)compareComparables.invoke(null, kc, k, pk)) == 0) {
                        if (!searched) {
                            Map.Entry<K, V> q, ch;
                            searched = true;
                            if (((ch = left(p)) != null &&
                                    (q = find(ch, h, k, kc, converter)) != null) ||
                                    ((ch = right(p)) != null &&
                                            (q = find(ch, h, k, kc, converter)) != null))
                                return q;
                        }
                        dir = tieBreakOrder(k, pk, converter);
                    }

                    Map.Entry<K, V> xp = p;
                    if ((p = (dir <= 0) ? left(p) : right(p)) == null) {
                        Entry<K, V> xpn = next(xp);
                        Class hashMapNode = Class.forName("java.util.HashMap$Node");
                        Method newTreeNode = ReflectUtils.getDeclareMehodByName(map.getClass(), "newTreeNode",
                                int.class, Object.class, Object.class, hashMapNode);
                        newTreeNode.setAccessible(true);
                        Map.Entry<K, V> x = (Map.Entry<K, V>)newTreeNode.invoke(map, h, k, v, xpn);
                        if (dir <= 0)
                            ReflectUtils.setPropertyByFieldName(xp, x, "left");
                        else
                            ReflectUtils.setPropertyByFieldName(xp, x, "right");
                        ReflectUtils.setPropertyByFieldName(xp, x, "next");
                        ReflectUtils.setPropertyByFieldName(x, xp, "parent");
                        ReflectUtils.setPropertyByFieldName(x, xp, "prev");
                        if (xpn != null)
                            ReflectUtils.setPropertyByFieldName(xpn, x, "prev");
                        moveRootToFront(tab, balanceInsertion(treeNode, root, x));
                        return null;
                    }
                }
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        static <K,V> Map.Entry<K,V> balanceInsertion(Map.Entry<K, V> treeNode, Map.Entry<K,V> root,
                                                            Map.Entry<K,V> x) {
            try {
                validateTreeNode(treeNode, root, x);
                Class treeNodeClass = Class.forName("java.util.HashMap$TreeNode");
                Method method = ReflectUtils.getDeclareMehodByName(treeNode.getClass(), "balanceInsertion", treeNodeClass, treeNodeClass);
                method.setAccessible(true);
                return (Map.Entry<K, V>)method.invoke(treeNode, root, x);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

         static <K, V> Map.Entry<K,V> getTreeNode( Map.Entry<K,V> treeNode, int h, Object k,  ArrayObjectConverter<K> converter) {
            return find(parent(treeNode) != null ? root(treeNode) : treeNode, h, k, null, converter);
        }

         static <K, V> void removeTreeNode(Map.Entry<K, V> treeNode,  HashMap<K,V> map, Map.Entry<K,V>[] tab,
                                                boolean movable) {
            try {
                Class nodeArray = Class.forName("[Ljava.util.HashMap$Node;");
                Method removeTreeNode = ReflectUtils.getDeclareMehodByName(treeNode.getClass(), "removeTreeNode", HashMap.class,nodeArray,boolean.class);
                removeTreeNode.setAccessible(true);
                removeTreeNode.invoke(treeNode, map, tab, movable);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }


        private static <K, V> void validateTreeNode(Map.Entry<K, V> ... treeNodes)  {
            try {
                if(null == treeNodes) {
                    throw new RuntimeException("parameter can not be null");
                }
                for (Entry<K, V> treeNode:treeNodes
                     ) {
                    if(!treeNode.getClass().getCanonicalName().equals("java.util.HashMap.TreeNode")) {
                        throw new RuntimeException("it is not tree node");
                    }
                }

            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private static <K, V> int hash(Map.Entry<K, V> treeNode) {
            return (int)ReflectUtils.getPropertyByFieldName(treeNode, "hash");
        }

        private static <K, V> Map.Entry<K, V> parent(Map.Entry<K, V> treeNode) {
            return (Map.Entry<K,V>)ReflectUtils.getPropertyByFieldName(treeNode, "parent");
        }

        private static <K, V> Map.Entry<K, V> prev(Map.Entry<K, V> treeNode) {
            return (Map.Entry<K,V>)ReflectUtils.getPropertyByFieldName(treeNode, "prev");
        }

        private static <K, V> Map.Entry<K, V> next(Map.Entry<K, V> treeNode) {
            return (Map.Entry<K,V>)ReflectUtils.getPropertyByFieldName(treeNode, "next");
        }

        private static <K, V> Map.Entry<K, V> left(Map.Entry<K, V> treeNode) {
            return (Map.Entry<K,V>)ReflectUtils.getPropertyByFieldName(treeNode, "left");
        }

        private static <K, V> Map.Entry<K, V> right(Map.Entry<K, V> treeNode) {
             return (Map.Entry<K,V>)ReflectUtils.getPropertyByFieldName(treeNode, "right");
        }

    }
}
