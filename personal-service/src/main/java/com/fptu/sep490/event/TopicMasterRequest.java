package com.fptu.sep490.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TopicMasterRequest(
        String operation,
        UUID taskId,
        String title,
        int type
) {
}
