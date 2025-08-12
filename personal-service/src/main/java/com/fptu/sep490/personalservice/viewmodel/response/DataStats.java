package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;

import java.util.List;

@Builder
public record DataStats(
        int numberOfTasks,
        int numberOfExams,
        int taskAttempted,
        int examAttempted,
        List<UserInBranch> userInBranchAvg,
        List<UserInBranch> userInBranchHighest,
        List<ReportQuestionTypeStats> questionTypeStats,
        List<ReportQuestionTypeStatsWrong> questionTypeStatsWrong
) {
}
