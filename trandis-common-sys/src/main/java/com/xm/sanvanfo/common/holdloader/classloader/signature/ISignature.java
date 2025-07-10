package com.xm.sanvanfo.common.holdloader.classloader.signature;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public interface ISignature {
    String sign(URL url) throws Exception;
    boolean checkSignature(URL url);

    default boolean isFile(URL url) throws URISyntaxException {
        return new File(url.toURI()).isFile();
    }

    default boolean isDirectory(URL url) throws URISyntaxException {
        return new File(url.toURI()).isDirectory();
    }
}
