package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;

import java.util.List;

public record AddGroupQuestionResponse(
        @JsonProperty("group_id")
        String groupId,
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
        @JsonProperty("drag_item")
        List<String> dragItem
) {}