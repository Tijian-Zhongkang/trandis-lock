package com.xm.sanvanfo.common.utils;

import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@SuppressWarnings({"unused"})
public class EncryptUtils {

    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F' };
    private static char[] encodeHex(byte[] data) {

        int l = data.length;

        char[] out = new char[l << 1];

        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }
    public static String encodeByMd5(String string) throws NoSuchAlgorithmException {
        // 确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Base64.Encoder base64Encoder = Base64.getEncoder();
        // 加密字符串
        return new String(encodeHex((md5.digest(string.getBytes(StandardCharsets.UTF_8))))).toLowerCase();
    }

    public static String encodeByMd5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        return new String(encodeHex(md5.digest(bytes))).toLowerCase();
    }

    public static String HMACSHA256(String data, String key) throws Exception {

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        sha256_HMAC.init(secret_key);

        byte[] array = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();

        for (byte item : array) {

            sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);

        }

        return sb.toString().toUpperCase();

    }

    public static byte[] xorEncrypt(byte[] orgin, String key) throws Exception {
        try(BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(orgin))) {
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                int b;
                while ((b = bis.read()) != -1) {
                    char[] ase = key.toCharArray();
                    int c = -1;
                    for (char ch : ase) {
                        c = (c == -1 ? b : c) ^ ch;
                    }
                    bos.write(c);
                }
                return bos.toByteArray();
            }
        }
    }

    public static String base64Encrypt(byte[] bytes) {
        sun.misc.BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(bytes);
    }

    public static byte[] base64Decoder(String s)  throws IOException {
        sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
        return decoder.decodeBuffer(s);
    }

    public static String base64Decoder(String s, String encode) throws IOException {
        sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
        byte[] b = decoder.decodeBuffer(s);
        return (new String(b, encode));
    }

    public static String javaBase64Encode(byte[] s) {
        return Base64.getEncoder().encodeToString(s);
    }

    public static String javaBase64Decode(String s, String encode) throws UnsupportedEncodingException  {
        byte[] b = Base64.getDecoder().decode(s.getBytes());
        return (new String(b, encode));
    }

    public static byte[] javaBase64Decode(String s) {
        return Base64.getDecoder().decode(s.getBytes());
    }

    public static String base16Decode(String theHex) {
        char[] chars = theHex.toCharArray();
        int len = chars.length / 2;
        byte[] theByte = new byte[len];

        for (int i = 0; i < len; i++) {
            theByte[i] = Integer.decode("0X" + chars[i*2] + chars[i*2+1]).byteValue();
        }

        return new String(theByte);
    }

    public static String base16Encode(String theStr) {
        int tmp;
        String tmpStr;
        byte[] bytes = theStr.getBytes();
        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte aByte : bytes) {
            tmp = aByte;
            if (tmp < 0) {
                tmp += 256;
            }

            tmpStr = Integer.toHexString(tmp);
            if (tmpStr.length() == 1) {
                result.append('0');
            }

            result.append(tmpStr);
        }

        return result.toString();
    }

    public static String sha1Encode(String str) throws NoSuchAlgorithmException {
        if(null == str) {
            return null;
        }
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[] bytes = sha1.digest(str.getBytes());
        return new String(encodeHex(bytes));
    }

    public static String urlEncode(String str, String encode) throws UnsupportedEncodingException {
        String result = URLEncoder.encode(str, encode);
        result = result.replaceAll("\\+", "%20");
        return result;
    }

    public static String urlDecode(String str, String encode) throws UnsupportedEncodingException {
        str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return URLDecoder.decode(str, encode);
    }
}
