package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fptu.sep490.personalservice.helper.LocalTimeDeserializer;
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
       @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
       @JsonDeserialize(using = LocalTimeDeserializer.class)
      LocalTime reminderTime,
      @JsonProperty("recurrence")
      Integer recurrence,
      @JsonProperty("time_zone")
      String timeZone,
      @JsonProperty("enable")
      Boolean enable,
       @JsonProperty("message")
       String message
) {
}
