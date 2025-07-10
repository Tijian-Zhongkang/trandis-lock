package com.xm.sanvanfo.common.utils;

@SuppressWarnings("unused")
public class Asserts {
    public static void noNull(Object obj) {
        if(null == obj) {
            throw new AssertException("obj is null");
        }
    }

    public static void isTrue(boolean result) {
        if(!result) {
            throw new AssertException("result is false");
        }
    }
}
