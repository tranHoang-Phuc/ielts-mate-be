package com.fptu.sep490.personalservice.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReminderConfig {
    @JsonProperty("reminder_time")
    LocalTime reminderTime;
}
