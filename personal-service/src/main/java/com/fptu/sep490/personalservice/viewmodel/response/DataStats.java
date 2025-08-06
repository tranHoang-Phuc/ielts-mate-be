package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;

@Builder
public record DataStats(
        int numberOfTasks,
        int numberOfExams,
        int taskAttempted,
        int examAttempted
) {
}
