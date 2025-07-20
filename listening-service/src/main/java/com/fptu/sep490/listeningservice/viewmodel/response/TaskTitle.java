package com.fptu.sep490.listeningservice.viewmodel.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskTitle(
        UUID taskId,
        String title
) {
}
