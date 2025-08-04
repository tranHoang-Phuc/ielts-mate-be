package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ModuleResponse(
        @JsonProperty("module_id")
        UUID moduleId,

        @JsonProperty("module_name")
        String moduleName,

        @JsonProperty("description")
        String description,

        @JsonProperty("is_public")
        Boolean isPublic,

        @JsonProperty("is_deleted")
        Boolean isDeleted,

        @JsonProperty("flash_card_ids")
        List<FlashCardResponse> flashCardIds,

        @JsonProperty("created_by")
        String createdBy,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_by")
        String updatedBy,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("time_spent")
        Long timeSpent,

        @JsonProperty("progress")
        Double progress




) {
}
