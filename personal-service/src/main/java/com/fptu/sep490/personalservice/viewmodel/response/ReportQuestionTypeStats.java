package com.fptu.sep490.personalservice.viewmodel.response;

public record ReportQuestionTypeStats(
        Integer questionType,
        long correctCount
) {
}
