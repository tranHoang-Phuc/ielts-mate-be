package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttemptResponse(
        @JsonProperty("attempt_id")
        UUID attemptId,
        @JsonProperty("listening_task_id")
        UUID taskId,
        @JsonProperty("ielts_type")
        int ieltsType,
        @JsonProperty("part_number")
        int partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        String title,
        @JsonProperty("audio_file_id")
        UUID audioFileId,
        @JsonProperty("question_groups")
        List<QuestionGroupAttemptResponse> questionGroups
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionGroupAttemptResponse(
            @JsonProperty("question_group_id")
            UUID questionGroupId,
            @JsonProperty("section_order")
            Integer sectionOrder,
            @JsonProperty("section_label")
            String sectionLabel,
            @JsonProperty("instruction")
            String instruction,
            @JsonProperty("sentence_with_blanks")
            String sentenceWithBlanks,
            @JsonProperty("questions")
            List<QuestionAttemptResponse> questions,
            @JsonProperty("drag_items")
            List<DragItemResponse> dragItems
    ) {
        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record QuestionAttemptResponse(
                @JsonProperty("question_id")
                UUID questionId,
                @JsonProperty("question_order")
                Integer questionOrder,
                @JsonProperty("question_type")
                Integer questionType,
                @JsonProperty("number_of_correct_answers")
                Integer numberOfCorrectAnswers,
                @JsonProperty("blank_index")
                Integer blankIndex,
                @JsonProperty("instruction_for_choice")
                String instructionForChoice,
                @JsonProperty("instruction_for_matching")
                String instructionForMatching,
                @JsonProperty("zone_index")
                Integer zoneIndex,
                @JsonProperty("choices")
                List<ChoiceAttemptResponse> choices
        ) {
            @Builder
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            public record ChoiceAttemptResponse(
                    @JsonProperty("choice_id")
                    UUID choiceId,
                    @JsonProperty("label")
                    String label,
                    @JsonProperty("content")
                    String content,
                    @JsonProperty("choice_order")
                    Integer choiceOrder
            ) {
            }
        }

        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record DragItemResponse (
                @JsonProperty("drag_item_id")
                String dragItemId,
                @JsonProperty("content")
                String content
        ) {

        }



    }
}
