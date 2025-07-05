package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record SavedAnswersRequest(
        @JsonProperty("question_id")
        UUID questionId,
        @JsonProperty("choices")
        List<UUID> choices,
        @JsonProperty("data_filled")
        String dataFilled,
        @JsonProperty("data_matched")
        String dataMatched,
        @JsonProperty("drag_item_id")
        UUID dragItemId
) {
}
