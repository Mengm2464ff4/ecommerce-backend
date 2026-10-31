package com.ecommerce.ecommerce_backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(AppConstants.DATE_TIME_FORMAT);
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(AppConstants.ISO_DATE_TIME_FORMAT);

    private DateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatIsoDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public static boolean isBeforeNow(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(now());
    }

    public static boolean isAfterNow(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(now());
    }

    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }
}
