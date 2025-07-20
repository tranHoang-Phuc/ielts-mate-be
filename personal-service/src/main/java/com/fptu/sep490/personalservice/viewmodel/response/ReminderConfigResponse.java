package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ReminderConfigResponse(
        @JsonProperty("config_id")
        Integer configId,
        @JsonProperty("email")
        String email,
        @JsonProperty("account_id")
        UUID accountId,
        @JsonProperty("message")
        String message,
        @JsonProperty("reminder_date")
        List<LocalDate> reminderDate,
        @JsonProperty("reminder_time")
        LocalTime reminderTime,
        @JsonProperty("zone")
        String zone,
        @JsonProperty("recurrence")
        Integer recurrence,
        boolean enabled) {
}
