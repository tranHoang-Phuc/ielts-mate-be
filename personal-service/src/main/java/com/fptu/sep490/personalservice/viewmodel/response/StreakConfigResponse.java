package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record StreakConfigResponse(
        @JsonProperty("start_date")
        LocalDate startDate,
        @JsonProperty("last_updated")
        LocalDate lastUpdated,
        @JsonProperty("streak")
        int currentStreak
) {
}
