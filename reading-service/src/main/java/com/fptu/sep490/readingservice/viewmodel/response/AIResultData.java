package com.fptu.sep490.readingservice.viewmodel.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AIResultData(
        Integer duration,
        Integer totalPoint,
        LocalDateTime createdAt
) {
}
