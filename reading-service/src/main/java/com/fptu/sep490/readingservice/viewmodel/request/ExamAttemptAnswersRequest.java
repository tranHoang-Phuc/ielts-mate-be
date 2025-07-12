package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ExamAttemptAnswersRequest(
        @JsonProperty(value ="passage_id")
        List<UUID> passageId,
        @JsonProperty( value= "question_group_ids")
        List<UUID> questionGroupIds,
        @JsonProperty("answers")
        List<ExamAnswerRequest> answers,
        @JsonProperty("duration")
        Integer duration
) {
    public record ExamAnswerRequest(
            @JsonProperty("question_id")
            UUID questionId,
            @JsonProperty("selected_answers")
            List<String> selectedAnswers
    ) {
    }
}
