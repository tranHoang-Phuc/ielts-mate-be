package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ExamRequest(
        @JsonProperty("exam_name")
        String examName,

        @JsonProperty("exam_description")
        String examDescription,

        @JsonProperty("url_slug")
        String urlSlug,

        @JsonProperty("part1_id")
        UUID part1Id,

        @JsonProperty("part2_id")
        UUID part2Id,

        @JsonProperty("part3_id")
        UUID part3Id,

        @JsonProperty("part4_id")
        UUID part4Id

) {
}
