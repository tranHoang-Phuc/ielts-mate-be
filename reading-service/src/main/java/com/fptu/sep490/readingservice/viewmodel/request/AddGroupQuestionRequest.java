package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record AddGroupQuestionRequest(
        @JsonProperty("section_order")
        Integer sectionOrder,
        @JsonProperty("section_label")
        String sectionLabel,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("question_type")
        int questionType,
        @JsonProperty("questions")
        List<QuestionCreationRequest> questions,
        @JsonProperty("drag_items")
        List<String> dragItems
) {
}