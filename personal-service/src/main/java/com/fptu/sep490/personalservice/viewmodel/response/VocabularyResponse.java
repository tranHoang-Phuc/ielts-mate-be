package com.fptu.sep490.personalservice.viewmodel.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record VocabularyResponse(
        @JsonProperty("vocabulary_id") UUID vocabularyId,
        @JsonProperty("word") String word,
        @JsonProperty("context") String context,
        @JsonProperty("meaning") String meaning,
        @JsonProperty("created_by") String createdBy,
        @JsonProperty("created_at") LocalDateTime createdAt

) {
}
