package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserGetHistoryExamAttemptResponse(
        @JsonProperty("exam_attempt_id")
        UUID examAttemptId,
        @JsonProperty("reading_exam")
        UserGetHistoryExamAttemptReadingExamResponse readingExam,
        @JsonProperty("duration")
        Integer duration,
        @JsonProperty("total_question")
        Integer totalQuestion,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("updated_by")
        UserInformationResponse updatedBy,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("updated_at")
        String updatedAt
) {
        @Builder
        public record UserGetHistoryExamAttemptReadingExamResponse(
                @JsonProperty("reading_exam_id")
                UUID readingExamId,
                @JsonProperty("reading_exam_name")
                String readingExamName,
                @JsonProperty("reading_exam_description")
                String readingExamDescription,
                @JsonProperty("url_slug")
                String urlSlug
        ) {
        }
}
