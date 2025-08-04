package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FlashCardProgressResponse(
        @JsonProperty("flashcard_id")
        String flashcardId, // ID of the flashcard
        @JsonProperty("status")
        Integer status, // 0: pending, 1: allowed, 2: denied
        @JsonProperty("flashcard_detail")
        FlashCardResponse flashcardDetail // Details of the flashcard

) {
}
