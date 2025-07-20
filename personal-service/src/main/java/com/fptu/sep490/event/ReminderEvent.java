package com.fptu.sep490.event;

import lombok.Builder;

import java.util.List;

@Builder
public record ReminderEvent(
        List<String> email,
        String subject,
        String htmlContent
) {
}
