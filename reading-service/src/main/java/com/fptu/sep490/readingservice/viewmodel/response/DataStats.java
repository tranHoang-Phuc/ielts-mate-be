package com.fptu.sep490.readingservice.viewmodel.response;

import lombok.Builder;

import java.util.List;

@Builder
public record DataStats(
        int numberOfTasks,
        int numberOfExams,
        int taskAttempted,
        int examAttempted,
        List<UserInBranch> userInBranchAvg,
        List<UserInBranch> userInBranchHighest
) {
}
