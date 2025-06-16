package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;

import java.util.List;

public record InformationUpdatedQuestionRequest(@JsonProperty("explanation")
    String explanation,
    @JsonProperty("point")
    Integer point,
    @JsonProperty("question_categories")
    List<String> questionCategories,
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
    @JsonProperty("drag_item_id")
    String dragItemId

) {
}
