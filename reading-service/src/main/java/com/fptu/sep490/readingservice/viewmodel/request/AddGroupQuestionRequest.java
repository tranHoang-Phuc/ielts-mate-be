package com.fptu.sep490.readingservice.viewmodel.request;

import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record AddGroupQuestionRequest(
        @JsonProperty("section_order")
        Integer sectionOrder,
        @JsonProperty("section_label")
        String sectionLabel,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("questions")
        List<QuestionCreationRequest> questions,
        @JsonProperty("drag_item")
        List<String> dragItem
) {

}