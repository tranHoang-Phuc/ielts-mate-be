package com.fptu.sep490.personalservice.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.*;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReminderConfig {
    @JsonProperty("message")
    String message;

    @JsonProperty("reminder_date")
    LocalDate reminderDate;

    @JsonProperty("reminder_time")
    LocalTime reminderTime;

    @JsonProperty("timezone")
    ZoneId timezone;

    @JsonProperty("recurrence")
    RecurrenceType recurrence;

    @JsonProperty("days_of_week")
    List<DayOfWeek> daysOfWeek;

    @JsonProperty("custom_rule")
    String customRule;

    @JsonProperty("enabled")
    boolean enabled;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
