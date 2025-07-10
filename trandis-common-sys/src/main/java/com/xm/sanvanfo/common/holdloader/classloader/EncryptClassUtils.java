package com.xm.sanvanfo.common.holdloader.classloader;

import com.xm.sanvanfo.common.holdloader.classloader.compiler.JdkCompilerUtils;
import com.xm.sanvanfo.common.utils.EncryptUtils;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings({"WeakerAccess", "unused"})
public class EncryptClassUtils {

    public static byte[] encryptXor(Path path, String key, boolean addKey, String out, String classPaths) throws Exception {
        out = out.replaceAll("\\\\", "/");
        if(!out.endsWith("/")) {
            out = out + "/";
        }
        ArrayList<String> options = new ArrayList<>();
        options.add("-source");
        options.add("1.8");
        options.add("-target");
        options.add("1.8");
        options.add("-d");
        options.add(out);
        if(null != classPaths) {
            options.add("-classpath");
            options.add(classPaths);
        }
        String classPath = JdkCompilerUtils.doCompile(options, path);
        String byteFile = out + classPath;
        byte[] bytes = Files.readAllBytes(new File(byteFile).toPath());
        return encryptXor(bytes, key, addKey);
    }

    public static byte[] encryptXor(Class cls, String key, boolean addKey) throws Exception{
        //Note that class may be dynamic loading, so getResource may be null.
        ClassPool pool = ClassPool.getDefault();
        URL resource = cls.getResource("");
        if(null == resource || resource.getProtocol().equals("jar") || resource.getProtocol().equals("file")) {
            URL url =  cls.getProtectionDomain().getCodeSource().getLocation();
            if(null != url) {
                pool.appendClassPath(url.getFile());
            }
            else if(UrlPathClassLoader.class.isAssignableFrom(cls.getClassLoader().getClass())) {
                String baseUrl = ((UrlPathClassLoader)cls.getClassLoader()).getBasePath();
                pool.appendClassPath(new URL(baseUrl.substring(0, baseUrl.length() - 1)).getFile());
            }
        }
        CtClass ctClass = pool.get(cls.getName());
        byte[] bytes = ctClass.toBytecode();
        //It may be encrypted. Judge the file header
        byte[] head = Arrays.copyOf(bytes, 6);
        String fileHead = new String(head, StandardCharsets.UTF_8);
        if(fileHead.equals("DCLASS")) {
            return bytes;
        }
        return encryptXor(bytes, key, addKey);
    }

    //The simple encrypted file format is roughly as follows
    //File header: file type (dClass: 6 bytes) + encryption key method (1 byte: 0: agreed encryption; 1: own encryption key)
    //If it is agreed encryption, the cipher text will be followed; If the encryption key comes with itself, the format
    // is: encryption key length (1 byte) + encryption key (n bytes)
    //XOR mode is adopted for encryption
    public static byte[] encryptXor(byte[] cls, String key, boolean addKey) throws Exception {
        byte[] encrypts = EncryptUtils.xorEncrypt(cls, key);
        byte[] newBytes;
        int length = 7;
        byte[] keyByte = key.getBytes(StandardCharsets.UTF_8);
        if(addKey) {
            newBytes = new byte[length + keyByte.length + 1 + encrypts.length];
        }
        else {
            newBytes = new byte[length + encrypts.length];
        }
        byte[] head = new byte[7];
        System.arraycopy("DCLASS".getBytes(StandardCharsets.UTF_8), 0, head, 0, 6);
        if(addKey) {
            head[6] = 1;
        }
        else {
            head[6] = 0;
        }
        System.arraycopy(head, 0, newBytes, 0, 7);
        if(addKey) {
            newBytes[7] = (byte)keyByte.length;
            System.arraycopy(keyByte, 0, newBytes, 8, keyByte.length);
            length = length + keyByte.length + 1;
        }
        System.arraycopy(encrypts, 0, newBytes, length, encrypts.length);
        return newBytes;
    }

    public static byte[] decodeXor(byte[] body, String privateKey) {
        try {
            byte[] fClass = Arrays.copyOf(body, 6);
            String tmp = new String(fClass, StandardCharsets.UTF_8);
            if(!tmp.equals("DCLASS")) {
                throw new RuntimeException("加密文件格式错误");
            }
            byte b = body[7];
            String encryptKey;
            byte[] classBody;
            if (b == 0) {
                encryptKey = privateKey;
                classBody = Arrays.copyOfRange(body, 7, body.length);
            } else {
                byte length = body[7];
                byte[] bKey = Arrays.copyOfRange(body, 8, length + 8);
                encryptKey = new String(bKey, StandardCharsets.UTF_8);
                classBody = Arrays.copyOfRange(body, length + 8, body.length);
            }
            return EncryptUtils.xorEncrypt(classBody, encryptKey);

        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
