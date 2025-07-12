package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QuestionGroupCreationRequest(
        @JsonProperty("listening_task_id")
        UUID listeningTaskId,
        @JsonProperty("section_order")
        Integer sectionOrder,
        @JsonProperty("section_label")
        String sectionLabel,
        @JsonProperty("instruction")
        String instruction
) {
}
