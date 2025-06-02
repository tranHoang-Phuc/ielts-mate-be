package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record PassageGetResponse(
        @JsonProperty("passage_id")
        String passageId,
        @JsonProperty("ielts_type")
        int ieltsType,
        @JsonProperty("part_number")
        int partNumber,
        @JsonProperty("passage_status")
        int passageStatus,
        @JsonProperty("title")
        String title,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("updated_by")
        UserInformationResponse updatedBy,
        String createdAt,
        String updatedAt
) {
}
