package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.util.Json;
import lombok.Builder;

@Builder
public record ReadingExamResponse(

        @JsonProperty("reading_exam_id")
        String readingExamId,
        @JsonProperty("reading_exam_name")
        String readingExamName,
        @JsonProperty("reading_exam_description")
        String readingExamDescription,
        @JsonProperty("url_slug")
        String urlSlug,
        @JsonProperty("reading_passage_id_part1")
        ReadingPassageResponse readingPassageIdPart1,
        @JsonProperty("reading_passage_id_part2")
        ReadingPassageResponse readingPassageIdPart2,
        @JsonProperty("reading_passage_id_part3")
        ReadingPassageResponse readingPassageIdPart3,
        @JsonProperty("is_marked_up")
        Boolean isMarkedUp,
        @JsonProperty("markup_type")
        Integer markupTypes
) {
    public record ReadingPassageResponse(
            @JsonProperty("reading_passage_id")
            String readingPassageId,
            @JsonProperty("reading_passage_name")
            String readingPassageName,
            @JsonProperty("reading_passage_content")
            String readingPassageContent
    ) {
    }
}
