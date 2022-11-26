package com.yuanfudao.aries.qa.btest.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public final static Calendar CALENDAR = Calendar.getInstance();
    public final static SimpleDateFormat SIMPLE_DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public final static SimpleDateFormat SIMPLE_DATE_FORMAT_2 = new SimpleDateFormat("MM.dd");

    public static long[] getStartEndMillTime(int range, TimeUnit timeUnit) {
        long currTime = System.currentTimeMillis();
        return new long[] {currTime - TimeUnit.MILLISECONDS.convert(range, timeUnit), currTime};
    }

    public static long[] getStartEndOfThisWeek() {

        Calendar calendar = Calendar.getInstance();
        // 一周第一天为周日，所以此处日+1
        if (calendar.get(Calendar.DAY_OF_WEEK) == 1) {
            calendar.setWeekDate(calendar.getWeekYear(), calendar.get(Calendar.WEEK_OF_YEAR) - 1, 2);
        } else {
            calendar.setWeekDate(calendar.getWeekYear(), calendar.get(Calendar.WEEK_OF_YEAR), 2);
        }
        calendar.set(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0);
        long start = calendar.getTimeInMillis();

        // 一周第一天为周日，所以此处为下一周第一天
        if (calendar.get(Calendar.DAY_OF_WEEK) == 1) {
            calendar.setWeekDate(calendar.getWeekYear(), calendar.get(Calendar.WEEK_OF_YEAR) + 1, 1);
        } else {
            calendar.setWeekDate(calendar.getWeekYear(), calendar.get(Calendar.WEEK_OF_YEAR) + 1, 1);
        }
        calendar.set(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                23, 59, 59);

        long end = calendar.getTimeInMillis();
        return new long[] {start, end};
    }

    public static String getStartEndDayOfWeek() {
        long[] se = getStartEndOfThisWeek();
        long start = se[0];
        long end = se[1];
        return getStartEndDayOfWeek(start, end);
    }

    public static String getStartEndDayOfWeek(long start, long end) {
        Date startDate = new Date(start);
        Date endDate = new Date(end);
        return SIMPLE_DATE_FORMAT_2.format(startDate) + "-" + SIMPLE_DATE_FORMAT_2.format(endDate);
    }

//    public static int getTimeOffset() {
//        LocalDateTime now = LocalDateTimeUtil.now();
//        LocalDateTime dt = LocalDateTimeUtil.offset(now, -3, ChronoUnit.HOURS);
//    }

    public static void main(String[] args) {
        System.out.println(getStartEndDayOfWeek());
    }
}
