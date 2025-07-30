package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ModuleProgressRequest(
        @JsonProperty("status")
        Integer status, // 0: pending, 1: allowed, 2: denied
        @JsonProperty("last_index_read")
        Integer lastIndexRead, // this is index that user last read in module, then when user open it, it will show the last read
        @JsonProperty("highlighted_flashcard_ids")
        List<String> highlightedFlashcardIds
) {
}
