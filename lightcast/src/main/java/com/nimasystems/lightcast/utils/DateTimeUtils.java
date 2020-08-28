package com.nimasystems.lightcast.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@SuppressLint("Assert")
public class DateTimeUtils {

    public static final String FORMAT_ISO_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_ISO_DATE = "yyyy-MM-dd";
    public static final String FORMAT_ISO_TIME = "HH:mm:ss";
    public static final String FORMAT_SHORT_TIME_TIME = "HH:mm";

    public static final String FORMAT_SQL_LONG = "yyyy-MM-dd kk:mm:ss";

    public static String dateToSQLStringFormat(Date date) {
        return dateToString(date, FORMAT_SQL_LONG);
    }

    public static String dateToShortSQLStringFormat(Date date) {
        return dateToString(date, FORMAT_ISO_DATE);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    public static String dateToHourMinutesStringFormat(Date date) {
        return dateToString(date, FORMAT_SHORT_TIME_TIME);
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public static String dateToUTCShortSqliteString(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd",
                Locale.ENGLISH);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        format.setTimeZone(tz);
        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToUTCSqliteString(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.ENGLISH);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        format.setTimeZone(tz);
        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date utcStringToDate(String dateStringInUtc) {
        if (dateStringInUtc == null) {
            return null;
        }
        String formatStr = dateStringInUtc.contains(" ") ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(formatStr,
                Locale.ENGLISH);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        format.setTimeZone(tz);

        try {
            return format.parse(dateStringInUtc);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date utcDateStringToDate(String dateStringInUtc) {
        if (dateStringInUtc == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd",
                Locale.ENGLISH);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        format.setTimeZone(tz);

        try {
            return format.parse(dateStringInUtc);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String millisecondsToDateStr(long milliseconds) {
        return millisecondsToDateStr(milliseconds, FORMAT_ISO_DATETIME);
    }

    public static String millisecondsToDateStr(long milliseconds, String format) {
        Date date = new Date(milliseconds);
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.US);
        return formatter.format(date);
    }

    public static String dateTimeToLocaleStringFormat(Context context, Date date) {
        if (date == null || context == null) {
            return null;
        }

        java.text.DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);

        java.text.DateFormat timeFormat = android.text.format.DateFormat
                .getTimeFormat(context);

        return dateFormat.format(date) + " " + timeFormat.format(date);
    }

    public static Integer daysBetweenDate(Date fromDateTime, Date toDateTime) {
        Calendar fromDay = Calendar.getInstance();
        fromDay.setTime(fromDateTime);

        Calendar toDay = Calendar.getInstance();
        toDay.setTime(toDateTime);

        long diff = toDay.getTimeInMillis() - fromDay.getTimeInMillis();
        long days = diff / (24 * 60 * 60 * 1000);

        return (int) days;
    }

    public static String dateToLocaleStringFormat(Context context, Date date) {
        if (date == null || context == null) {
            return null;
        }

        java.text.DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);
        return dateFormat.format(date);
    }

    public static String dateToString(Date date, String format) {
        if (date == null || StringUtils.isNullOrEmpty(format)) {
            return null;
        }
        return DateFormat.format(format, date).toString();
    }

    public static Date getDateFromUnixtime(int unixtime) {
        long time = unixtime * (long) 1000;
        return new Date(time);
    }

    public static long getUnixTimestamp(Date date, boolean toCurrentTimezone) {
        if (date == null) {
            return 0;
        }

        long unix_timestamp;

        if (toCurrentTimezone) {
            int gmtOffset = TimeZone.getDefault().getRawOffset();
            int dstOffset = TimeZone.getDefault().getDSTSavings();
            unix_timestamp = (date.getTime() + gmtOffset + dstOffset) / 1000;
        } else {
            unix_timestamp = (date.getTime()) / 1000;
        }
        return unix_timestamp;
    }

    public static String getSystemDateFormat(Context context, String dateString) {
        if (context == null || StringUtils.isNullOrEmpty(dateString)) {
            assert false;
            return null;
        }
        java.text.DateFormat df = new SimpleDateFormat(FORMAT_SQL_LONG,
                Locale.US);
        Date date;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        if (date != null) {
            java.text.DateFormat dateFormat = android.text.format.DateFormat
                    .getDateFormat(context);
            return dateFormat.format(date) + " " + dateString.substring(11, 16);
        }
        return null;
    }

    public static long startOfDay(Date d) {
        return startOfDayMils(d, null);
    }

    public static long startOfDayMils(Date d, TimeZone tz) {
        Calendar cal = (tz != null ? Calendar.getInstance(tz) : Calendar
                .getInstance());
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    public static Date stringToDateTime(String dateString, Locale locale) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                locale);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date stringToTime2(String dateString, Locale locale, TimeZone tz) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", locale);
        format.setTimeZone(tz);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date stringToTime2WithLocalTimezone(String dateString, Locale locale) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", locale);
        format.setTimeZone(TimeZone.getDefault());

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date stringToTime2(String dateString, Locale locale) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", locale);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date stringToTime(String dateString, Locale locale) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", locale);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date stringToDate(String dateString, Locale locale) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", locale);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String dateToSqliteString(Date date, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                locale);
        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateTimeToString(Date date, String format, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat fmt = new SimpleDateFormat(format,
                locale);
        try {
            return fmt.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateTimeToString(Date date, String format) {
        return dateTimeToString(date, format, Locale.getDefault());
    }

    public static String dateTimeToStringWithTimezone(TimeZone timezone,
                                                      Date date, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                locale);
        format.setTimeZone(timezone);
        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToStringWithTimezone(TimeZone timezone, Date date,
                                                  Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", locale);
        format.setTimeZone(timezone);
        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToString(Date date, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", locale);

        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String timeToString(Date date, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", locale);

        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String timeToString2(Date date, Locale locale) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", locale);

        try {
            return format.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getMinsFromDate(Date date) {

        Calendar lCal = Calendar.getInstance();
        lCal.setTime(date);

        int hours = lCal.get(Calendar.HOUR_OF_DAY);
        if (hours == 0) {
            hours = 12;
        }

        int mins = lCal.get(Calendar.MINUTE);

        return 60 * hours + mins;
    }

    public static String dateFromLocalToUTCTimezone(Date inputDate,
                                                    String format) {
        if (inputDate != null) {
            SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format, Locale.ENGLISH);

            sourceFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sourceFormat1.format(inputDate);
        } else {
            return "";
        }
    }

    public static String dateFromUTCToLocalTimezone(String dateStr,
                                                    String format) {
        if (StringUtils.isNullOrEmpty(dateStr) || StringUtils.isNullOrEmpty(format)) {
            return null;
        }

        SimpleDateFormat sourceFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateUTC;
        String date;

        try {
            dateUTC = sourceFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format, Locale.ENGLISH);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        sourceFormat1.setTimeZone(tz);
        date = sourceFormat1.format(dateUTC);

        return date;
    }

    public static Date dateFromUTCToLocalTimezoneDate(String dateStr,
                                                      String format) {
        return stringToDateTime(dateFromUTCToLocalTimezone(dateStr, format), Locale.ENGLISH);
    }

    public static Date dateFromLocalToUTCTimezoneDate(Date inputDate,
                                                      String format) {
        return stringToDateTime(dateFromLocalToUTCTimezone(inputDate, format), Locale.ENGLISH);
    }

    @SuppressLint("SimpleDateFormat")
    public static String dateFromTimezoneToUTC(String dateStr, String format,
                                               TimeZone serverTimezone) {

        SimpleDateFormat sourceFormat = new SimpleDateFormat(format);

        if (serverTimezone != null) {
            sourceFormat.setTimeZone(serverTimezone);
        }

        Date dateServerTimezone;
        String dateUTC;
        try {
            dateServerTimezone = sourceFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        sourceFormat1.setTimeZone(tz);
        dateUTC = sourceFormat1.format(dateServerTimezone);

        return dateUTC;
    }

    @SuppressLint("SimpleDateFormat")
    public static String dateFromUTCTimeZoneToCustom(String dateStr,
                                                     String format) {

        SimpleDateFormat sourceFormat = new SimpleDateFormat(format);
        sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateUTC;
        String date;
        try {
            dateUTC = sourceFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        sourceFormat1.setTimeZone(tz);
        date = sourceFormat1.format(dateUTC);

        return date;
    }

    @SuppressLint("SimpleDateFormat")
    public static String dateFromCustomTimeZoneToUTC(String dateStr,
                                                     String format) {

        SimpleDateFormat sourceFormat = new SimpleDateFormat(format);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        sourceFormat.setTimeZone(tz);
        Date dateUTC;
        String date;
        try {
            dateUTC = sourceFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format);
        sourceFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
        date = sourceFormat1.format(dateUTC);

        return date;
    }

    @SuppressLint("SimpleDateFormat")
    public static String dateFromUTCTimeZoneToDefault(String dateStr,
                                                      String format, TimeZone tz) {

        SimpleDateFormat sourceFormat = new SimpleDateFormat(format);
        sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateUTC;
        String date;
        try {
            dateUTC = sourceFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        SimpleDateFormat sourceFormat1 = new SimpleDateFormat(format);
        sourceFormat1.setTimeZone(tz);
        date = sourceFormat1.format(dateUTC);

        return date;
    }
}
