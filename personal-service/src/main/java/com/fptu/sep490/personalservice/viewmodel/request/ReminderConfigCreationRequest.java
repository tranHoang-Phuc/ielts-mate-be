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
       // nếu recurrence = NONE, Thì date chỉ cho user chọn 1 ngày duy nhất
      // nếu recurrence = CUSTOME, thì date có thể là nhiều ngày
      // neewus recurrence = DAILY,WEEKLY, MOnthly, yearly mặc định là ngày hôm nay
      List<LocalDate> reminderDate,
       @JsonProperty("reminder_time")
       @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
       @JsonDeserialize(using = LocalTimeDeserializer.class)
       //Nhận thời gian 24h format HH:mm
      LocalTime reminderTime,
      @JsonProperty("recurrence")
      // 0: NONE, 1: DAILY, 2: WEEKLY, 3: MONTHLY, 4: YEARLY, 5: CUSTOM
      Integer recurrence,
      @JsonProperty("time_zone")
      String timeZone,
      @JsonProperty("enable")
      Boolean enable,
       @JsonProperty("message")
       String message
) {
}
