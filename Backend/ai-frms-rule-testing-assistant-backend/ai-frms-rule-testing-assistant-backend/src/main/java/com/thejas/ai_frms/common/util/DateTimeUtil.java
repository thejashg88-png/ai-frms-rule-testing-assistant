package com.thejas.ai_frms.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDateTime minusHours(long hours) {
        return LocalDateTime.now().minusHours(hours);
    }

    public static LocalDateTime minusDays(long days) {
        return LocalDateTime.now().minusDays(days);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parse(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTime, ISO_DATE_TIME_FORMATTER);
    }

    public static OffsetDateTime parseOffsetDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(dateTime, ISO_OFFSET_DATE_TIME_FORMATTER);
    }
}