package com.fptu.sep490.listeningservice.viewmodel.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateExamAttemptResponse(
        @JsonProperty("exam_attempt_id")
        UUID examAttemptId,
        @JsonProperty("listening_exam")
        ListeningExamResponse listeningExam,
        @JsonProperty("url_slug")
        String urlSlug,
        @JsonProperty("total_question")
        Integer totalQuestion,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("created_at")
        String createdAt
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ListeningExamResponse(
            @JsonProperty("listening_exam_id")
            UUID listeningExamId,
            @JsonProperty("listening_exam_name")
            String listeningExamName,
            @JsonProperty("listening_exam_description")
            String listeningExamDescription,
            @JsonProperty("url_slug")
            String urlSlug,
            @JsonProperty("listening_task_id_part1")
            ListeningTaskResponse listeningTaskIdPart1,
            @JsonProperty("listening_task_id_part2")
            ListeningTaskResponse listeningTaskIdPart2,
            @JsonProperty("listening_task_id_part3")
            ListeningTaskResponse listeningTaskIdPart3
    ) {
        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record ListeningTaskResponse(
                @JsonProperty("task_id")
                UUID taskId,
                @JsonProperty("ielts_type")
                Integer ieltsType,
                @JsonProperty("part_number")
                Integer partNumber,
                @JsonProperty("instruction")
                String instruction,
                @JsonProperty("title")
                String title,
                @JsonProperty("audio_file_id")
                UUID audioFileId,

                @JsonProperty("question_groups")
                List<AttemptResponse.QuestionGroupAttemptResponse> questionGroups
        ) {
        }
    }
}
