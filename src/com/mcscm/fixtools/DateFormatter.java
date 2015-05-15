package com.mcscm.fixtools;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatter {

    public static DateFormat DATE_FORMAT = createDateFormat("yyyyMMdd");
    public static DateFormat DATE_TIME_FORMAT = createDateFormat("yyyyMMdd-HH:mm:ss");
    public static DateFormat DATE_TIME_FORMAT_MILIS = createDateFormat("yyyyMMdd-HH:mm:ss.SSS");
    public static DateFormat TIME_FORMAT = createDateFormat("HH:mm:ss");
    public static DateFormat TIME_FORMAT_MILLIS = createDateFormat("HH:mm:ss");


    private static DateFormat createDateFormat(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
        return sdf;
    }

    public static String formatAsDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static String formatAsDateTime(Date date) {
        return DATE_TIME_FORMAT.format(date);
    }

    public static String formatAsDateTimeMilis(Date date) {
        return DATE_TIME_FORMAT_MILIS.format(date);
    }

    public static String formatAsTime(Date date) {
        return DATE_TIME_FORMAT.format(date);
    }

    public static Date parseDate(String value) {
        try {
            return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date parseDateTime(String value) {
        try {
            return DATE_TIME_FORMAT.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date parseDateTimeMilis(String value) {
        try {
            return DATE_TIME_FORMAT_MILIS.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date parseTime(String value) {
        try {
            return TIME_FORMAT.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
