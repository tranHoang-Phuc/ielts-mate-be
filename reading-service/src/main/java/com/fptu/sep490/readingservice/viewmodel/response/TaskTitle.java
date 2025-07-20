package com.fptu.sep490.readingservice.viewmodel.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskTitle(
        UUID taskId,
        String title
) {
}
