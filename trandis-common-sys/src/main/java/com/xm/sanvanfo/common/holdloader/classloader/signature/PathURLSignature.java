package com.xm.sanvanfo.common.holdloader.classloader.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xm.sanvanfo.common.utils.EncryptUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class PathURLSignature implements ISignature {

    private final ObjectMapper mapper;

    PathURLSignature() {
        mapper = new ObjectMapper();
    }

    @Override
    public String sign(URL url) throws Exception {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        iterate(url, map);
        byte[] bytes = mapper.writeValueAsBytes(map);
        return EncryptUtils.encodeByMd5(bytes);
    }

    @Override
    public boolean checkSignature(URL url) {
        try {
            return (url.getProtocol().equalsIgnoreCase("file") || StringUtils.isEmpty(url.getProtocol()))
                    && !isDirectory(url);
        }
        catch (URISyntaxException ignore) {
            return false;
        }
    }

    protected void iterate(URL url, LinkedHashMap<String, Object> map)
            throws IOException, URISyntaxException, NoSuchAlgorithmException {
         List<Path> paths = Files.walk(Paths.get(url.toURI())).sorted((u, v) -> {
             File uF = u.toFile();
             File vF = v.toFile();
             if (uF.isDirectory() && vF.isFile()) {
                 return -1;
             } else if (uF.isFile() && vF.isDirectory()) {
                 return 1;
             }
             return uF.getName().compareTo(vF.getName());
         }).collect(Collectors.toList());
        for (Path path:paths
             ) {
            if(path.toFile().isFile()) {
                byte[] content = getContent(path.toFile().toURI().toURL());
                map.put(path.toFile().getName(), EncryptUtils.encodeByMd5(content));
            }
            else if(path.toFile().isDirectory()) {
                LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
                map.put(path.toFile().getName(), newMap);
                iterate(path.toFile().toURI().toURL(), newMap);
            }
        }
    }

    protected byte[] getContent(URL url) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(url.toURI()));
    }
}
