package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QuestionGroupCreationRequest(
        @JsonProperty("section_order")
        Integer sectionOrder,
        @JsonProperty("section_label")
        String sectionLabel,
        @JsonProperty("question_type")
        int questionType,
        @JsonProperty("instruction")
        String instruction
) {
}
