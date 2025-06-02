package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

public record QuestionCreationRequest(
        @JsonProperty("question_order")
        Integer questionOrder,
        @JsonProperty("point")
        Integer point,
        @JsonProperty("question_type")
        Integer questionType,
        @JsonProperty("question_category")
        Set<String> questionCategory,
        @JsonProperty("explanation")
        String explanation,
        @JsonProperty("number_of_correct_answers")
        Integer numberOfCorrectAnswers,
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
        @JsonProperty("choices")
        List<ChoiceCreationRequest> choices,
        @JsonProperty("drag_item")
        String dragItem
) {}