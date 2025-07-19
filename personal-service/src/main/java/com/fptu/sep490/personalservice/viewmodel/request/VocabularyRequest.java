package com.fptu.sep490.personalservice.viewmodel.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record VocabularyRequest(
        @JsonProperty("word")
        String word,
        @JsonProperty("context")
        String context,
        @JsonProperty("meaning")
        String meaning
) {
}
