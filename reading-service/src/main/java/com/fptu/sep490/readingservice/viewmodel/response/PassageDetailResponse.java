package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PassageDetailResponse(
        @JsonProperty("passage_id")
        String passageId,
        @JsonProperty("ielts_type")
        int ieltsType,
        @JsonProperty("part_number")
        int partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        String title,
        @JsonProperty("content")
        String content,
        @JsonProperty("content_with_highlight_keywords")
        String contentWithHighlightKeywords,
        @JsonProperty("passage_status")
        int passageStatus,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("updated_by")
        UserInformationResponse updatedBy,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("updated_at")
        String updatedAt,

        @JsonProperty("question_groups")
        List<PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse> questionGroups
) {
}
