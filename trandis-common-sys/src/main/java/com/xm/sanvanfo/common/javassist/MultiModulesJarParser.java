package com.xm.sanvanfo.common.javassist;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

@Slf4j
public class MultiModulesJarParser {
    private static final String bootInfoClassesFolder = "BOOT-INF/classes/";
    private final String jarFilePath;

    public MultiModulesJarParser(URL url) throws IOException {
        if(url.getProtocol().equals("jar")) {
            url = new URL(url.getFile());
        }
        if(url.getProtocol().equals("file")) {
            String filePath = url.getFile();
            if(filePath.endsWith("!/")) {
                filePath = filePath.substring(0, filePath.length() - 2);
            }
            File file = new File(filePath);
            if(file.isFile()) {
                jarFilePath = filePath;
                return;
            }
        }
        throw new RuntimeException("非法的本地文件格式");
    }

    @SuppressWarnings("unused")
    public InputStream openInputStream(Function<JarEntryPath, Boolean> func, Boolean extraJar, String libFilter) throws IOException {
        return openInputStreamPrivate(func, extraJar, libFilter);
    }

    public InputStream openSpringJarInputStream(String className, Boolean extraJar, String libFilter) throws IOException {
        return openInputStreamPrivate(o->getClassName(o.getEntryName()).equals(className), extraJar, libFilter);
    }

    private InputStream openInputStreamPrivate(Function<JarEntryPath, Boolean> func, Boolean extraJar, String libFilter) throws IOException {
        try(JarFile jarFile = new JarFile(new File(jarFilePath))) {
            JarEntryPath path = findEntryPath(func, jarFile, extraJar, libFilter);
            if(null == path) {
                return null;
            }

            if(path.getBasePath().size() == 0) {
                return jarFile.getInputStream(jarFile.getEntry(path.getEntryName()));
            }
            Iterator<String> iterator = path.getBasePath().iterator();
            String subJarPath = iterator.next();
            InputStream in = jarFile.getInputStream(jarFile.getEntry(subJarPath));
            while(true) {
                if (!iterator.hasNext() || null == in) {
                    return parseJarStream(in, path.getEntryName());
                } else {
                    in = parseJarStream(in, iterator.next());
                }
            }

        }
    }

    private InputStream parseJarStream(InputStream in, String next) throws IOException {
        if(null == in) {
            return null;
        }
        try(JarInputStream inputStream = new JarInputStream(in)) {

            do {
                JarEntry jarEntry = inputStream.getNextJarEntry();
                if(null == jarEntry) {
                    return null;
                }
                if(jarEntry.getName().equals(next)) {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        byte[] tmp = new byte[256];
                        int chunk, read = 0;
                        while (-1 != (chunk = inputStream.read(tmp))) {
                            outputStream.write(tmp, 0, chunk);
                            read += chunk;
                        }
                        log.debug("{}的长度为{}字节", next, read);
                        return new ByteArrayInputStream(outputStream.toByteArray());
                    }
                }
            }
            while (true);
        }
        finally {
            in.close();
        }
    }

    private JarEntryPath findEntryPath(Function<JarEntryPath, Boolean> func, JarFile jarfile, Boolean extraJar, String libFilter) throws IOException {
        File file = new File(jarFilePath);
        String localFile =  file.getCanonicalFile().toURI().toURL().toString();
        for (JarEntry entry:Collections.list(jarfile.entries())
             ) {
            JarEntryPath path = getEntryPath(func, entry, new ArrayList<>(), localFile, jarfile, extraJar, libFilter);
            if(null != path) {
                return path;
            }
        }
        return null;
    }

    private JarEntryPath getEntryPath(Function<JarEntryPath, Boolean> func, JarEntry entry, List<String> list, String localFile, JarFile jarFile, Boolean extraJar, String libFilter) throws IOException {
        String name = entry.getName();

        if(name.endsWith(".jar")) {
            boolean ret = extraJar && (StringUtils.isEmpty(libFilter) || Pattern.compile(libFilter).matcher(name).find());
            if(ret) {
                try(InputStream in = jarFile.getInputStream(entry)) {
                    try (JarInputStream inputStream = new JarInputStream(in)) {
                        JarEntry jarEntry;
                        do {
                            jarEntry = inputStream.getNextJarEntry();

                            if (null != jarEntry) {
                                String newFile = String.format("%s!/%s", localFile, name);
                                List<String> newList = new ArrayList<>(list);
                                newList.add(name);
                                JarEntryPath find = getEntryPath(func, jarEntry, newList, newFile, jarFile, true, libFilter);
                                if(null != find) {
                                    return find;
                                }
                            }
                        }
                        while (null != jarEntry);
                    }
                }
            }
        }
        if(!entry.isDirectory()) {
            URL value = new URL(String.format("jar:%s!/%s", localFile, name));
            JarEntryPath path = new JarEntryPath();
            path.setBasePath(list);
            path.setEntryName(name);
            path.setUrl(value);
            if(func.apply(path)) {
                return path;
            }

        }
        return null;
    }

    private String getClassName(String name) {
        name = name.substring(0, name.length() - 6);
        if(name.startsWith(bootInfoClassesFolder)) {
            name = name.substring(bootInfoClassesFolder.length());
        }
        return name.replace('/', '.');
    }

    @Data
    private static class JarEntryPath {
        private List<String> basePath;
        private String entryName;
        private URL url;
    }

}
