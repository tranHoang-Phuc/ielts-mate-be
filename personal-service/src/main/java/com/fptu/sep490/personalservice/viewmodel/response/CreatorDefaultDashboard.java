package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CreatorDefaultDashboard(
        @JsonProperty("number_of_reading_tasks")
        int numberOfReadingTasks,

        @JsonProperty("number_of_listening_tasks")
        int numberOfListeningTasks,

        @JsonProperty("number_of_reading_exams")
        int numberOfReadingExams,

        @JsonProperty("number_of_listening_exams")
        int numberOfListeningExams,

        @JsonProperty("users_in_avg_branch_score_reading")
        List<UserBranchScore> userInAvgBranchScoreReading,

        @JsonProperty("users_in_highest_branch_score_reading")
        List<UserBranchScore> userInHighestBranchScoreReading,

        @JsonProperty("users_in_avg_branch_score_listening")
        List<UserBranchScore> userInAvgBranchScoreListening,

        @JsonProperty("users_in_highest_branch_score_listening")
        List<UserBranchScore> userInHighestBranchScoreListening
) {
}
