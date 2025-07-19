package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record ReminderConfigCreationRequest(
       @Email
       @JsonProperty("email")
      String email,
      @JsonProperty("reminder_date")
      List<LocalDate> reminderDate,
      @JsonProperty("reminder_time")
      LocalTime reminderTime,
      @JsonProperty("recurrence")
      Integer recurrence,
      @JsonProperty("days_of_week")
      List<DayOfWeek> daysOfWeek,
      @JsonProperty("time_zone")
      String timeZone,
      @JsonProperty("enable")
      Boolean enable
) {
}
