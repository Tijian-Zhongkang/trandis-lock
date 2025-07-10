package com.xm.sanvanfo.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

@SuppressWarnings({"unused"})
public class ConsistentHashSelector {

    private final SortedMap<Long, Object> virtualNodes = new TreeMap<>();
    private HashFunction hashFunction;

    private ConsistentHashSelector() {
    }


    public ConsistentHashSelector setHashFunction(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
        return this;
    }

    public ConsistentHashSelector setNodes(Object[] nodes, int virtualNodeNum) {
        if(null != hashFunction) {
            hashFunction = new MD5Hash();
        }
        for (Object node : nodes) {
            for (int i = 0; i < virtualNodeNum; i++) {
                virtualNodes.put(hashFunction.hash(node.toString() + i), node);
            }
        }
        return this;
    }

    public Object select(String objectKey) {
        if(null == hashFunction) {
            throw new RuntimeException("you must set nodes first");
        }
        SortedMap<Long, Object> tailMap = virtualNodes.tailMap(hashFunction.hash(objectKey));
        Long nodeHashVal = tailMap.isEmpty() ? virtualNodes.firstKey() : tailMap.firstKey();
        return virtualNodes.get(nodeHashVal);
    }

    public static class Builder {

        public static ConsistentHashSelector create() {
            return new ConsistentHashSelector();
        }
    }

    private static class MD5Hash implements HashFunction {
        MessageDigest instance;
         private MD5Hash() {
            try {
                instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        @Override
        public long hash(String key) {
            instance.reset();
            instance.update(key.getBytes());
            byte[] digest = instance.digest();
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        }
    }

    public interface HashFunction {
        long hash(String key);
    }
}
