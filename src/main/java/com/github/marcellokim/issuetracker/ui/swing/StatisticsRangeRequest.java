package com.github.marcellokim.issuetracker.ui.swing;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

record StatisticsRangeRequest(
        LocalDate dailyFromInclusive,
        LocalDate dailyToInclusive,
        YearMonth monthlyFromInclusive,
        YearMonth monthlyToInclusive) {

    static StatisticsRangeRequest allTime() {
        return new StatisticsRangeRequest(null, null, null, null);
    }

    static StatisticsRangeRequest parse(
            String dailyFromText,
            String dailyToText,
            String monthlyFromText,
            String monthlyToText) {
        LocalDate dailyFrom = parseDate(dailyFromText, "Daily from");
        LocalDate dailyTo = parseDate(dailyToText, "Daily to");
        YearMonth monthlyFrom = parseMonth(monthlyFromText, "Monthly from");
        YearMonth monthlyTo = parseMonth(monthlyToText, "Monthly to");
        requireOrdered(dailyFrom, dailyTo, "Daily from must be <= daily to.");
        requireOrdered(monthlyFrom, monthlyTo, "Monthly from must be <= monthly to.");
        return new StatisticsRangeRequest(dailyFrom, dailyTo, monthlyFrom, monthlyTo);
    }

    private static LocalDate parseDate(String text, String fieldName) {
        String value = normalized(text);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-MM-dd.", exception);
        }
    }

    private static YearMonth parseMonth(String text, String fieldName) {
        String value = normalized(text);
        if (value == null) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-MM.", exception);
        }
    }

    private static <T extends Comparable<T>> void requireOrdered(T fromInclusive, T toInclusive, String message) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.compareTo(toInclusive) > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String normalized(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
