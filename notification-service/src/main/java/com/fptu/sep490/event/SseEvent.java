package com.fptu.sep490.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SseEvent(
        UUID clientId, String message, String status
) {
}
