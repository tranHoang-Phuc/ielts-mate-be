package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record UserDataAttempt(
        @JsonProperty("attempt_id")
        UUID attemptId,
        @JsonProperty("answers")
        List<AnswerChoice> answers,
        @JsonProperty("duration")
        Long duration
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
