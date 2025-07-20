package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record MarkUpResponse(
        @JsonProperty("markup_id")
       Integer markUpId,
       @JsonProperty("markup_type")
       Integer markupType,
       @JsonProperty("task_type")
       Integer taskType,
       @JsonProperty("practice_type")
       Integer practiceType,
       @JsonProperty("task_title")
       String taskTitle

) {
}
