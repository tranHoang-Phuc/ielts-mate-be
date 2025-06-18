package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CustGetListQuestionsByGroupIdResponse(
    @JsonProperty("group_id") String groupId,
    @JsonProperty("questions")
    List<Question> questions
) {
        public record Question(
                @JsonProperty("question_id") String questionId,
                @JsonProperty("question_order") int questionOrder,
                @JsonProperty("point") int point,
                @JsonProperty("question_type") int questionType,
                @JsonProperty("question_category") List<String> questionCategory,
                @JsonProperty("number_of_correct_answer") int numberOfCorrectAnswer,
                @JsonProperty("choice") List<Choice> choice,
                @JsonProperty("blank_index") Integer blankIndex,
                @JsonProperty("instruction_for_matching") String instructionForMatching,
                @JsonProperty("zone_index") Integer zoneIndex
        ) {
            public record Choice(
                    @JsonProperty("choice_id") String choiceId,
                    @JsonProperty("label") String label,
                    @JsonProperty("content") String content,
                    @JsonProperty("choice_order") int choiceOrder
            ) {}
        }
    }
