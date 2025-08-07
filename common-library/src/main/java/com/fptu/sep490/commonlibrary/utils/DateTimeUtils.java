package com.fptu.sep490.commonlibrary.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

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

    /**
     * Normalize một ngày về “đầu” của bucket theo timeFrame:
     *
     * @param date       ngày cần normalize
     * @param timeFrame  chuỗi như "1d","3d","1w","1m","1y"
     * @param startDate  mốc bắt đầu dùng cho chunk n-day (dạng DAYS với value>1)
     * @return ngày đại diện cho bucket chứa date
     */
    public static LocalDate normalize(
            LocalDate date,
            String timeFrame,
            LocalDate startDate
    ) {
        Objects.requireNonNull(date,      "date must not be null");
        Objects.requireNonNull(timeFrame,"timeFrame must not be null");

        // parse value và unit
        int n = Integer.parseInt(timeFrame.substring(0, timeFrame.length() - 1));
        char unit = Character.toLowerCase(timeFrame.charAt(timeFrame.length() - 1));

        switch (unit) {
            case 'd':
                if (n <= 1) {
                    // daily: giữ nguyên
                    return date;
                } else {
                    // chunk n-day kể từ startDate
                    long daysFromStart = ChronoUnit.DAYS.between(startDate, date);
                    long idx = daysFromStart >= 0
                            ? daysFromStart / n
                            : -(( -daysFromStart + n - 1) / n);
                    return startDate.plusDays(idx * n);
                }
            case 'w':
                // tuần: về thứ Hai của tuần đó
                return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case 'm':
                // tháng: về ngày 1 cùng tháng
                return date.withDayOfMonth(1);
            case 'y':
                // năm: về ngày 1 tháng 1 cùng năm
                return date.withDayOfYear(1);
            default:
                throw new IllegalArgumentException(
                        "Unsupported timeFrame unit: " + unit +
                                " (must be d, w, m, or y)"
                );
        }
    }
}