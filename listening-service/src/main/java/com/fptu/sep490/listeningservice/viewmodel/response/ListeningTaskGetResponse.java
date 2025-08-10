package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListeningTaskGetResponse(
        @JsonProperty("task_id")
        UUID taskId,
        @JsonProperty("ielts_type")
        Integer ieltsType,
        @JsonProperty("part_number")
        Integer partNumber,
        @JsonProperty("status")
        Integer status,
        @JsonProperty("title")
        String title,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("updated_by")
        UserInformationResponse updatedBy,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("updated_at")
        String updatedAt,
        @JsonProperty("is_marked_up")
        Boolean isMarkedUp,
        @JsonProperty("markup_type")
        Integer markupTypes
) {
}
