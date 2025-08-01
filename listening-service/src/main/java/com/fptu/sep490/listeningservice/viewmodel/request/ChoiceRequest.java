package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ChoiceRequest(

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
