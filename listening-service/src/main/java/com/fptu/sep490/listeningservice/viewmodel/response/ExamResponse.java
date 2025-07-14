package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExamResponse(
        @JsonProperty("listening_exam_id")
        UUID listeningExamId,

        @JsonProperty("exam_name")
        String examName,

        @JsonProperty("exam_description")
        String examDescription,

        @JsonProperty("url_slug")
        String urlSlug,

        @JsonProperty("part1")
        ListeningTaskResponse part1,

        @JsonProperty("part2")
        ListeningTaskResponse part2,

        @JsonProperty("part3")
        ListeningTaskResponse part3,

        @JsonProperty("part4")
        ListeningTaskResponse part4,

        @JsonProperty("created_by")
        String createdBy,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_by")
        String updatedBy,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("is_current")
        Boolean isCurrent,

        @JsonProperty("display_version")
        Integer version,

        @JsonProperty("is_original")
        Boolean isOriginal,

        @JsonProperty("is_deleted")
        Boolean isDeleted

) {
}
