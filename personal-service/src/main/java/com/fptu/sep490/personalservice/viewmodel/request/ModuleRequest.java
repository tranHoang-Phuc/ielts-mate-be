package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ModuleRequest(
        @JsonProperty("module_name")
        String moduleName,
        @JsonProperty("module_description")
        String moduleDescription,
        @JsonProperty("vocabulary_ids")
        List<UUID> vocabularyIds,
        @JsonProperty("is_public")
        Boolean isPublic




) {
}
