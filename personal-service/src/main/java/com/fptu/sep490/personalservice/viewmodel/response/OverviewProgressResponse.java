package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OverviewProgressResponse(
        @JsonProperty("reading")
        ProgressOverview reading,
        @JsonProperty("listening")
        ProgressOverview listening,
        @JsonProperty("band_stats")
        BandStats bandStats,
        @JsonProperty("last_learning_date")
        LocalDateTime lastLearningDate
) {
    @Builder
    public record ProgressOverview(
            Integer exam,
            Integer task,
            @JsonProperty("total_exams")
            Integer totalExams,
            @JsonProperty("total_tasks")
            Integer totalTasks
    ) {
    }

    @Builder
    public record BandStats(
            @JsonProperty("start_date")
            LocalDateTime startDate,
            @JsonProperty("end_date")
            LocalDateTime endDate,
            @JsonProperty("time_frame")
            String timeFrame,
            @JsonProperty("average_reading_band")
            Double averageReadingBand,
            @JsonProperty("average_listening_band")
            Double averageListeningBand,
            @JsonProperty("average_overall_band")
            Double averageOverallBand,
            @JsonProperty("number_of_reading_exams")
            Integer numberOfReadingExams,
            @JsonProperty("number_of_listening_exams")
            Integer numberOfListeningExams
    ) {
    }
}
