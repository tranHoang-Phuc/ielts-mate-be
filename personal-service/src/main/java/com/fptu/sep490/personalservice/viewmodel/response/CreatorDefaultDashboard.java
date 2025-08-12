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


        @JsonProperty("users_in_avg_branch_score_listening")
        List<UserBranchScore> userInAvgBranchScoreListening,

        @JsonProperty("question_type_stats_reading")
        List<QuestionTypeStats> questionTypeStatsReading,

        @JsonProperty("question_type_stats_listening")
        List<QuestionTypeStats> questionTypeStatsListening,

        @JsonProperty("question_type_stats_reading_wrong")
        List<QuestionTypeStatsWrong> questionTypeStatsReadingWrong,

        @JsonProperty("question_type_stats_listening_wrong")
        List<QuestionTypeStatsWrong> questionTypeStatsListeningWrong


) {
}
