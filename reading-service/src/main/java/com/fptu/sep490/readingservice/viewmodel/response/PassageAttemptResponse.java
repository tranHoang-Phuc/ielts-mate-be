package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record PassageAttemptResponse(
        @JsonProperty("attempt_id")
        String attemptId,
        @JsonProperty("duration")
        Integer duration,
        @JsonProperty("total_points")
        Integer totalPoints,

        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("finished_at")
        String finishedAt,

        @JsonProperty("reading_passage")
        ReadingPassageResponse readingPassage
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record ReadingPassageResponse(
            @JsonProperty("passage_id")
            String passageId,
            @JsonProperty("instruction")
            String instruction,
            @JsonProperty("title")
            String title,
            @JsonProperty("content")
            String content,
            @JsonProperty("part_number")
            int partNumber,
            @JsonProperty("question_groups")
            List<QuestionGroupResponse> questionGroups
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Builder
        public record QuestionGroupResponse(
                @JsonProperty("group_id")
                String groupId,
                @JsonProperty("section_label")
                String sectionLabel,
                @JsonProperty("section_order")
                Integer sectionOrder,
                @JsonProperty("instruction")
                String instruction,
                @JsonProperty("drag_items")
                List<UpdatedQuestionResponse.DragItemResponse> dragItems,
                @JsonProperty("questions")
                List<QuestionResponse> questions
        ) {
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @Builder
            public record QuestionResponse(
                    @JsonProperty("question_id")
                    String questionId,
                    @JsonProperty("question_order")
                    int questionOrder,
                    @JsonProperty("question_type")
                    int questionType,
                    @JsonProperty("question_categories")
                    int numberOfCorrectAnswers,
                    String instructionForChoice,
                    List<UpdatedQuestionResponse.ChoiceResponse> choices,
                    Integer blankIndex,
                    String correctAnswer,
                    String instructionForMatching,
                    String correctAnswerForMatching,
                    Integer zoneIndex
            ) {

            }
        }
    }
}
