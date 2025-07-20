package com.fptu.sep490.personalservice.helper;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

public class UtcConverter {
    public static class UtcResult {
        private final List<LocalDate> utcDates;
        private final LocalTime utcTime;

        public UtcResult(List<LocalDate> utcDates, LocalTime utcTime) {
            this.utcDates = utcDates;
            this.utcTime = utcTime;
        }

        public List<LocalDate> getUtcDates() {
            return utcDates;
        }

        public LocalTime getUtcTime() {
            return utcTime;
        }
    }

    public static UtcResult convertToUtc(
            String timeZone,
            List<LocalDate> reminderDates,
            LocalTime reminderTime
    ) {
        ZoneId userZone = ZoneId.of(timeZone);

        List<LocalDate> utcDates = reminderDates.stream()
                .map(date -> {
                    ZonedDateTime userZdt = ZonedDateTime.of(date, reminderTime, userZone);
                    ZonedDateTime utcZdt  = userZdt.withZoneSameInstant(ZoneOffset.UTC);
                    return utcZdt.toLocalDate();
                })
                .collect(Collectors.toList());

        LocalTime utcTime = ZonedDateTime
                .of(reminderDates.get(0), reminderTime, userZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalTime();

        return new UtcResult(utcDates, utcTime);
    }
}
