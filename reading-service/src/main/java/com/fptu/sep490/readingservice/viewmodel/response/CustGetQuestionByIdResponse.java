package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record CustGetQuestionByIdResponse(
        @JsonProperty("question_id")       String questionId,
        @JsonProperty("question_order")    int    questionOrder,
        @JsonProperty("point")             int    point,
        @JsonProperty("question_type")     int    questionType,
        @JsonProperty("question_category") List<String> questionCategory,
        @JsonProperty("number_of_correct_answer") int numberOfCorrectAnswer,
        @JsonProperty("choice")            List<Choice> choice,
        @JsonProperty("blank_index")       Integer blankIndex,
        @JsonProperty("instruction_for_matching") String instructionForMatching,
        @JsonProperty("zone_index")        Integer zoneIndex
) {
    @Builder
    public record Choice(
            @JsonProperty("choice_id")    String choiceId,
            @JsonProperty("label")        String label,
            @JsonProperty("content")      String content,
            @JsonProperty("choice_order") int    choiceOrder
    ) {}
}
