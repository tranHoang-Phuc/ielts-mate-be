package com.fptu.sep490.personalservice.viewmodel.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FlashCardResponse(
        @JsonProperty("flashcard_id")
        String flashCardId,
        @JsonProperty("vocab")
        VocabularyResponse vocabularyResponse
) {
}
