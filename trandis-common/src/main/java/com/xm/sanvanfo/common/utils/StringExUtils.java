package com.xm.sanvanfo.common.utils;

@SuppressWarnings({"unused", "WeakerAccess"})
public class StringExUtils {

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isDouble(String value) {
        try {
            if(value.endsWith("f") || value.endsWith("F")) {
                value = value.substring(0, value.length() - 1);
            }
            Double.parseDouble(value);
            return value.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isLong(String value) {
        try {

            if(value.endsWith("l") || value.endsWith("L")) {
                value = value.substring(0, value.length() - 1);
            }
            Long.parseLong(value);
            return true;
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isNumber(String value) {
        return isInteger(value) || isDouble(value) || isLong(value);
    }
}
