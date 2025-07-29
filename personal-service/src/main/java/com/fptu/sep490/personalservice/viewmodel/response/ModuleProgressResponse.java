package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ModuleProgressResponse(
        @JsonProperty("id")
        String id,
        @JsonProperty("module_id")
        String moduleId,
        @JsonProperty("module_name")
        String moduleName,
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("status")
        Integer status, // 0: pending, 1: allowed, 2: denied
        @JsonProperty("last_index_read")
        Integer lastIndexRead, // this is index that user last read in module, then when user open it, it will show the last read
        @JsonProperty("highlighted_flashcard_ids")
        List<String> highlightedFlashcardIds

) {
}
