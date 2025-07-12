package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DragItemResponse(
        @JsonProperty("drag_item_id")
        String dragItemId,
        @JsonProperty("content")
        String content,
        @JsonProperty("question_group")
        QuestionGroupResponse questionGroup
) {
}
