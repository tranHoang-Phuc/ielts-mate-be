package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ExamAttemptAnswersRequest(
        @JsonProperty(value ="task_id")
        List<UUID> taskId,
        @JsonProperty( value= "question_group_ids")
        List<UUID> questionGroupIds,
        @JsonProperty( value= "item_ids")
        List<UUID> itemsIds,
        @JsonProperty("answers")
        List<ExamAnswerRequest> answers,
        @JsonProperty("duration")
        Integer duration
) {
    public record ExamAnswerRequest(
            @JsonProperty("question_id")
            UUID questionId,
            @JsonProperty("selected_answers")
            List<String> selectedAnswers,
            @JsonProperty("choice_ids")
            List<UUID>  choiceIds
    ) {
    }
}
