package com.fptu.sep490.notificationservice.viewmodel.event.consume;

import lombok.Builder;

@Builder
public record Recipient(
        String name,
        String email
) {
}
