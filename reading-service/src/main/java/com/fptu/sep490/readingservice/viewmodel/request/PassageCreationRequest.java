package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

public record PassageCreationRequest(
        @JsonProperty("ielts_type")
        int ieltsType,
        @JsonProperty("part_number")
        int partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        @NonNull
        String title,
        @JsonProperty("content")
        @NonNull
        String content,
        @JsonProperty("content_with_highlight_keywords")
        @NonNull
        String contentWithHighlightKeywords,
        @JsonProperty("passage_status")
        int passageStatus
) {
}
