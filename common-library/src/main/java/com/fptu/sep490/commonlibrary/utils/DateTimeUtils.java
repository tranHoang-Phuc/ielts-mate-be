package com.fptu.sep490.commonlibrary.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private DateTimeUtils() {
    }

    private static final String DEFAULT_PATTERN = "dd-MM-yyyy_HH-mm-ss";

    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_PATTERN);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    /**
     * Tính toán startDate dựa trên endDate và timeFrame kiểu "7d", "2w", "3m", "1y"
     * @param timeFrame chuỗi biểu thị khoảng thời gian, ví dụ: "7d", "2w", "1m", "1y"
     * @return LocalDateTime của startDate
     */
    public static LocalDateTime calculateStartDateFromTimeFrame(String timeFrame) {
        LocalDateTime endDate = LocalDateTime.now();
        if (timeFrame == null || timeFrame.length() < 2) {
            return endDate.minusWeeks(1); // mặc định nếu không hợp lệ
        }

        String unit = timeFrame.substring(timeFrame.length() - 1);
        String numberPart = timeFrame.substring(0, timeFrame.length() - 1);

        int value;
        try {
            value = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return endDate.minusWeeks(1); // fallback
        }

        return switch (unit) {
            case "d" -> endDate.minusDays(value);
            case "w" -> endDate.minusWeeks(value);
            case "m" -> endDate.minusMonths(value);
            case "y" -> endDate.minusYears(value);
            default -> endDate.minusWeeks(1); // fallback nếu unit không hợp lệ
        };
    }
}