package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListeningTaskGetAllResponse(
        @JsonProperty("listening_task_id")
        UUID taskId,
        @JsonProperty("ielts_type")
        Integer ieltsType,
        @JsonProperty("part_number")
        Integer partNumber,
        @JsonProperty("status")
        Integer status,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        String title,
        @JsonProperty("audio_file_id")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        UUID audioFileId,
        @JsonProperty("transcript")
        String transcription,
        @JsonProperty("question_groups")
        List<QuestionGroupResponse> questionGroups
    )
{
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionGroupResponse(
            @JsonProperty("group_id")
            UUID groupId,

            @JsonProperty("section_order")
            Integer sectionOrder,

            @JsonProperty("section_label")
            String sectionLabel,

            @JsonProperty("instruction")
            String instruction,

            @JsonProperty("drag_items")
            List<DragItemResponse> dragItems,

            @JsonProperty("questions")
            List<QuestionResponse> questions

    )
    {
        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record QuestionResponse(
                @JsonProperty("question_id")
                UUID questionId,
                @JsonProperty("question_order")
                int questionOrder,
                @JsonProperty("question_type")
                int questionType,
                @JsonProperty("point")
                int point,
                @JsonProperty("explanation")
                String explanation,
                @JsonProperty("number_of_correct_answers")
                int numberOfCorrectAnswers,
                @JsonProperty("instruction_for_choice")
                String instructionForChoice,
                @JsonProperty("choices")
                List<ChoiceResponse> choices,
                @JsonProperty("blank_index")
                Integer blankIndex,
                @JsonProperty("correct_answer")
                String correctAnswer,
                @JsonProperty("instruction_for_matching")
                String instructionForMatching,
                @JsonProperty("correct_answer_for_matching")
                String correctAnswerForMatching,
                @JsonProperty("zone_index")
                Integer zoneIndex,
                @JsonProperty("drag_item_id")
                UUID dragItemId
        ) {
            @Builder
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            public record ChoiceResponse(
                    @JsonProperty("choice_id")
                    UUID choiceId,
                    @JsonProperty("label")
                    String label,
                    @JsonProperty("choice_order")
                    int choiceOrder,
                    @JsonProperty("content")
                    String content,
                    @JsonProperty("is_correct")
                    Boolean isCorrect
            ){}
        }


        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record DragItemResponse(
                @JsonProperty("drag_item_id")
                UUID dragItemId,

                @JsonProperty("content")
                String content
        ){}
    }
}
