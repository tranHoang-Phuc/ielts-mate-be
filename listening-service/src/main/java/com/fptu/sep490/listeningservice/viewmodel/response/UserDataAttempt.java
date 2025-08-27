package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record UserDataAttempt(
        @JsonProperty("attempt_id")
        UUID attemptId,

        @JsonProperty("duration")
        Long duration,

        @JsonProperty("total_points")
        Integer totalPoints,

        @JsonProperty("task_data")
        ListeningTaskGetAllResponse attemptResponse,

        @JsonProperty("answers")
        List<AnswerChoice> answers

) {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder
    public record AnswerChoice(
            @JsonProperty("question_id")
            UUID questionId,
            @JsonProperty("choice_ids")
            List<UUID> choiceIds,
            @JsonProperty("filled_text_answer")
            String filledTextAnswer,
            @JsonProperty("matched_text_answer")
            String matchedTextAnswer,
            @JsonProperty("drag_item_id")
            UUID dragItemId
    ) {
    }
}
