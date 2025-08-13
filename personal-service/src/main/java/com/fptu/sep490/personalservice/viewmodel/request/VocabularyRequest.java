package com.fptu.sep490.personalservice.viewmodel.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.personalservice.model.enumeration.LangGuage;
import lombok.Builder;

@Builder
public record VocabularyRequest(
        @JsonProperty("word")
        String word,
        @JsonProperty("context")
        String context,
        @JsonProperty("meaning")
        String meaning,
        @JsonProperty("is_public")
        Boolean isPublic,
        @JsonProperty("language")
        LangGuage language // Assuming Language is an enum or class defined elsewhere
) {
}
