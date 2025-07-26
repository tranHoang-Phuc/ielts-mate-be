package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FlashCardRequest(
    @JsonProperty("vocab_id")
    String vocabId






) {
}
