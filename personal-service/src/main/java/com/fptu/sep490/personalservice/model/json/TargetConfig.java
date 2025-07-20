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
public class TargetConfig {
    @JsonProperty("listening_target")
    float listeningTarget;
    @JsonProperty("listening_target_date")
    LocalDate listeningTargetDate;

    @JsonProperty("reading_target")
    float readingTarget;
    @JsonProperty("reading_target_date")
    LocalDate readingTargetDate;
}
