package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FlashcardProgressRequest(
        @JsonProperty("flashcard_id")
        @NotBlank(message = "Flashcard ID is required")
        String flashcardId,
        
        @JsonProperty("is_correct")
        @NotNull(message = "Is correct flag is required")
        Boolean isCorrect,
        
        @JsonProperty("time_spent")
        @NotNull(message = "Time spent is required")
        Integer timeSpent // time spent in seconds
) {
}