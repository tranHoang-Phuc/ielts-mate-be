package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatedChoiceRequest(
        @JsonProperty("label")
        String label,
        @JsonProperty("content")
        String content,
        @JsonProperty("choice_order")
        Integer choiceOrder,
        @JsonProperty("is_correct")
        Boolean isCorrect
) {
}
