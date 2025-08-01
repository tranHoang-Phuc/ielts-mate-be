package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ChoiceResponse(
        @JsonProperty("choice_id")
        String choiceId,
        @JsonProperty("label")
        String label,
        @JsonProperty("content")
        String content,
        @JsonProperty("choice_order")
        Integer choiceOrder,
        @JsonProperty("is_correct")
        Boolean isCorrect,
        @JsonProperty("question_id")
        String questionId
) {
}
