package com.fptu.sep490.personalservice.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreakConfig {
    @JsonProperty("start_date")
    LocalDate startDate;
    @JsonProperty("last_updated")
    LocalDate lastUpdated;
}
