package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskTitle(
        UUID taskId,
        String title
) {
}
