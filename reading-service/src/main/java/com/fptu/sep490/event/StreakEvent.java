package com.fptu.sep490.event;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record StreakEvent(
        UUID accountId,
        LocalDate currentDate
) {
}
