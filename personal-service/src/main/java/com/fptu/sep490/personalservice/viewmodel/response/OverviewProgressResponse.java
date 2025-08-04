package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Builder
@Schema(description = "Thông tin tiến độ học tập của người dùng, bao gồm Reading và Listening")
public record OverviewProgressResponse(
        @Schema(description = "Thông tin tiến độ phần Reading")
        @JsonProperty("reading")
        ProgressOverview reading,

        @Schema(description = "Thông tin tiến độ phần Listening")
        @JsonProperty("listening")
        ProgressOverview listening,

        @Schema(description = "Thống kê điểm band theo khoảng thời gian")
        @JsonProperty("band_stats")
        BandStats bandStats,

        @Schema(description = "Thời điểm học gần nhất: lấy gần nhất trong các bài thi Reading và Listening, có thể là null nếu chưa học bao giờ")
        @JsonProperty("last_learning_date")
        LocalDateTime lastLearningDate
) {

    @Builder
    public record ProgressOverview(
            @Schema(description = "Số bài thi đã làm")
            Integer exam,

            @Schema(description = "Số task đã hoàn thành")
            Integer task,

            @Schema(description = "Tổng số bài thi")
            @JsonProperty("total_exams")
            Integer totalExams,

            @Schema(description = "Tổng số task")
            @JsonProperty("total_tasks")
            Integer totalTasks
    ) {}

    @Builder
    public record BandStats(
            @Schema(description = "Ngày bắt đầu thống kê")
            @JsonProperty("start_date")
            LocalDateTime startDate,

            @Schema(description = "Ngày kết thúc thống kê")
            @JsonProperty("end_date")
            LocalDateTime endDate,

            @Schema(description = "Khoảng thời gian đã chọn, ví dụ: 1d, 1w, 1m")
            @JsonProperty("time_frame")
            String timeFrame,

            @Schema(description = "Band trung bình phần Reading, có thể là null nếu không có bài thi nào trong khoảng thời gian")
            @JsonProperty("average_reading_band")
            Double averageReadingBand,

            @Schema(description = "Band trung bình phần Listening, có thể là null nếu không có bài thi nào trong khoảng thời gian")
            @JsonProperty("average_listening_band")
            Double averageListeningBand,

            @Schema(description = "Band trung bình tổng thể, có thể là null, nếu 1 trong 2 lis hoặc read thiếu thì chỉ lấy TB 1 phần còn lại")
            @JsonProperty("average_overall_band")
            Double averageOverallBand,

            @Schema(description = "Số bài thi Reading")
            @JsonProperty("number_of_reading_exams")
            Integer numberOfReadingExams,

            @Schema(description = "Số bài thi Listening")
            @JsonProperty("number_of_listening_exams")
            Integer numberOfListeningExams
    ) {}
}
