package com.xm.sanvanfo.common.holdloader.classloader.signature;

import com.xm.sanvanfo.common.utils.EncryptUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("WeakerAccess")
public class FileURLSignature implements ISignature {

    @Override
    public String sign(URL url) throws Exception {
        byte[] content = getContent(url);
        return EncryptUtils.encodeByMd5(content);
    }

    @Override
    public boolean checkSignature(URL url) {
        try {
            return (url.getProtocol().equalsIgnoreCase("file") || StringUtils.isEmpty(url.getProtocol()))
                    && isFile(url);
        }
        catch (URISyntaxException ignore) {
            return false;
        }
    }

    protected byte[] getContent(URL url) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(url.toURI()));
    }

}
