package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record QuestionCreationResponse(
    @JsonProperty("question_id")
    String questionId,
    @JsonProperty("question_order")
    int questionOrder,
    @JsonProperty("point")
    int point,
    @JsonProperty("question_type")
    int questionType,
    @JsonProperty("question_categories")
    List<String> questionCategories,
    @JsonProperty("explanation")
    String explanation,
    @JsonProperty("question_group_id")
    String questionGroupId,
    @JsonProperty("number_of_correct_answers")
    int numberOfCorrectAnswers,

    @JsonProperty("choices")
    List<ChoiceResponse> choices,
    @JsonProperty("instruction_for_choice")
    String instructionForChoice,

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
    @JsonProperty("drag_items")
    List<DragItemResponse> dragItems,

    @JsonProperty("created_by")
    UserInformationResponse createdBy,
    @JsonProperty("updated_by")
    UserInformationResponse updatedBy,
    @JsonProperty("created_at")
    String createdAt,
    @JsonProperty("updated_at")
    String updatedAt
) {
    public record ChoiceResponse(
        @JsonProperty("choice_id")
        String choiceId,
        @JsonProperty("label")
        String label,
        @JsonProperty("choice_order")
        int choiceOrder,
        @JsonProperty("content")
        String content,
        @JsonProperty("is_correct")
        boolean isCorrect
    ) {
    }

    @Builder
    public record DragItemResponse(
        @JsonProperty("drag_item_id")
        String dragItemId,
        @JsonProperty("content")
        String content
    ) {
    }
}
