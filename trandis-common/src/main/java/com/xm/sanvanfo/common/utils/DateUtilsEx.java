package com.xm.sanvanfo.common.utils;

import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtilsEx {

    private static String[] parsePatterns = {"yyyy-MM-dd","yyyy年MM月dd日",
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyyMMdd"};

    public static Date addDay(Date date, Integer n, int type) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(type, n);
        return c.getTime();
    }

    public static Date parseDate(String str) {
        try {
            if(null == str) {
                return null;
            }
            return DateUtils.parseDate(str, parsePatterns);
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static String dateToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static String dateToString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static Date convertFromTimestamp(Integer time) {
        return new Date(time.longValue() * 1000);
    }

    public static Date getLastMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, -1);
        return c.getTime();
    }

    public static Date getLastDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }

    public static Date getNextMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    public static Date getNextDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }
}
