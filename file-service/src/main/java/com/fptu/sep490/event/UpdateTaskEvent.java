package com.fptu.sep490.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateTaskEvent(
        UUID taskId,
        UUID fileId
) {
}
