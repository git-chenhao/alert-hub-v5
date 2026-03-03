package com.alerthub.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间工具类
 */
public final class DateUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() {
    }

    /**
     * 解析 ISO 8601 时间字符串
     */
    public static LocalDateTime parseIsoDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            // 尝试其他常见格式
            try {
                return LocalDateTime.parse(dateTimeStr.replace("T", " "),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    /**
     * 格式化为 ISO 字符串
     */
    public static String formatIsoDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * 格式化为显示字符串
     */
    public static String formatDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * 获取当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 计算时间窗口结束时间
     */
    public static LocalDateTime calculateWindowEnd(LocalDateTime start, int windowMinutes) {
        return start.plusMinutes(windowMinutes);
    }

    /**
     * 判断时间是否在窗口内
     */
    public static boolean isInWindow(LocalDateTime time, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return !time.isBefore(windowStart) && !time.isAfter(windowEnd);
    }
}
