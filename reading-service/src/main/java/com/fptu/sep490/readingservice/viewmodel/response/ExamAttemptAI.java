package com.fptu.sep490.readingservice.viewmodel.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExamAttemptAI(
        UUID examAttemptId,
        Integer duration,
        Integer totalPoint,
        List<String> topics,
        LocalDateTime createdAt
) {
}
