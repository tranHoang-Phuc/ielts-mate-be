package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserGetHistoryExamAttemptResponse(
        @JsonProperty("exam_attempt_id")
        UUID examAttemptId,
        @JsonProperty("listening_exam")
        UserGetHistoryExamAttemptListeningExamResponse listeningExam,
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
    public record UserGetHistoryExamAttemptListeningExamResponse(
            @JsonProperty("listening_exam_id")
            UUID listeningExamId,
            @JsonProperty("listening_exam_name")
            String listeningExamName,
            @JsonProperty("listening_exam_description")
            String listeningExamDescription,
            @JsonProperty("url_slug")
            String urlSlug
    ) {
    }
}
