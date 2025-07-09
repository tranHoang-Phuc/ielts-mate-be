package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateExamAttemptResponse(
        @JsonProperty("exam_attempt_id")
        UUID examAttemptId,
        @JsonProperty("reading_exam")
        ReadingExamResponse readingExam,
        @JsonProperty("url_slug")
        String urlSlug,
//        @JsonProperty("duration")
//        Long duration,
//        @JsonProperty("history")
//        String history,
        @JsonProperty("total_question")
        Integer totalQuestion,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("created_at")
        String createdAt
) {
        @Builder
        public record ReadingExamResponse(
                @JsonProperty("reading_exam_id")
                UUID readingExamId,
                @JsonProperty("reading_exam_name")
                String readingExamName,
                @JsonProperty("reading_exam_description")
                String readingExamDescription,
                @JsonProperty("url_slug")
                String urlSlug,
                @JsonProperty("reading_passage_id_part1")
                ReadingPassageResponse readingPassageIdPart1,
                @JsonProperty("reading_passage_id_part2")
                ReadingPassageResponse readingPassageIdPart2,
                @JsonProperty("reading_passage_id_part3")
                ReadingPassageResponse readingPassageIdPart3
        ) {
                @Builder
                public record ReadingPassageResponse(
                        @JsonProperty("passage_id")
                        UUID passageId,
                        @JsonProperty("instruction")
                        String instruction,
                        @JsonProperty("title")
                        String title,
                        @JsonProperty("content")
                        String content,
                        @JsonProperty("part_number")
                        int partNumber,

                        @JsonProperty("question_groups")
                        List<AttemptResponse.QuestionGroupAttemptResponse> questionGroups
                ) {
                }
        }

}
