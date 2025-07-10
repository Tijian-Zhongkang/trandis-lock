package com.xm.sanvanfo.common.utils;


import java.util.UUID;
@SuppressWarnings({"unused", "WeakerAccess"})
public class CommonUtils {

    public static String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "");
    }

    public static String pascalName(String content) {
        if(null == content || content.length() == 0) {
            return content;
        }
        if(content.length() == 1) {
            return content.toUpperCase();
        }
        return content.substring(0, 1).toUpperCase() + content.substring(1);
    }

    public static String lowerFirstLetter(String str) {

        if (str == null  || str.length() == 0) {
            return null;
        } else {

            return str.toLowerCase().substring(0, 1) + str.substring(1);
        }
    }

    public static Class<?> getCurrentThreadClass(String name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static String DbNameToClassName(String name) {
        if(!name.contains("_")) {
            return name.substring(0,1).toUpperCase() + name.substring(1);
        }
        String[] chs = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String ch:chs
        ) {
            sb.append(pascalName(ch));
        }
        return sb.toString();
    }


}
