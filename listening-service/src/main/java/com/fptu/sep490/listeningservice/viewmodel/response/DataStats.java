package com.fptu.sep490.listeningservice.viewmodel.response;

import lombok.Builder;

@Builder
public record DataStats(
        int numberOfTasks,
        int numberOfExams
) {
}
