package com.xm.sanvanfo.common.holdloader.classloader;

public interface IEncryptInterface {

    default byte[] Decode(byte[] body, String privateKey) {
        return EncryptClassUtils.decodeXor(body, privateKey);
    }
}
