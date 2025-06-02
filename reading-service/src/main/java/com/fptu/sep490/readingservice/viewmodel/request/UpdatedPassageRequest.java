package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatedPassageRequest(
        @JsonProperty("title")
        String title,
        @JsonProperty("ielts_type")
        Integer ieltsType,
        @JsonProperty("part_number")
        Integer partNumber,
        @JsonProperty("content")
        String content,
        @JsonProperty("content_with_highlight_keywords")
        String contentWithHighlightKeywords,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("passage_status")
        Integer passageStatus
) {
}
