package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionCreationRequest(
        @JsonProperty("question_order")
        int questionOrder,
        @JsonProperty("point")
        int point,
        @JsonProperty("question_type")
        int questionType,
        @JsonProperty("question_group_id")
        String questionGroupId,
        @JsonProperty("question_categories")
        List<String> questionCategories,
        @JsonProperty("explanation")
        String explanation,
        @JsonProperty("number_of_correct_answers")
        int numberOfCorrectAnswers,

        @JsonProperty("instruction_for_choice")
        String instructionForChoice,
        @JsonProperty("choices")
        List<ChoiceRequest> choices,

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
        String dragItemId
) {
    public record ChoiceRequest(
            @JsonProperty("label")
            String label,
            @JsonProperty("content")
            String content,
            @JsonProperty("choice_order")
            int choiceOrder,
            @JsonProperty("is_correct")
            boolean isCorrect
    ) {
    }

}
