package com.fptu.sep490.notificationservice.viewmodel.event;

import lombok.Builder;

@Builder
public record BaseMessageResponse(
        String status,
        String message
) {

}
