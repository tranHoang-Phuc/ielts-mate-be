package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ChoiceCreation(
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
