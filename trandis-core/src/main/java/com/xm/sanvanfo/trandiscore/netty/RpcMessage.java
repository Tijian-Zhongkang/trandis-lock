package com.xm.sanvanfo.trandiscore.netty;

import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public class RpcMessage {

    private int id;
    private byte messageType;
    private byte codec;
    private byte compressor;
    private final Map<String, String> headMap = new HashMap<>();
    private int bodyType;
    private Object body;

    @SuppressWarnings({"unused"})
    public String getHead(String headKey) {
        return headMap.get(headKey);
    }

    @SuppressWarnings({"unused"})
    public void putHead(String headKey, String headValue) {
        headMap.put(headKey, headValue);
    }

    public Map<String, String> getHeadMap() {
        return Collections.unmodifiableMap(headMap);
    }
}
