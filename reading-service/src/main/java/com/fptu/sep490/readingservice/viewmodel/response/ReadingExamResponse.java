package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadingExamResponse(

        @JsonProperty("reading_exam_id")
        String readingExamId,
        @JsonProperty("reading_exam_name")
        String readingExamName,
        @JsonProperty("reading_exam_description")
        String readingExamDescription,
        @JsonProperty("url_slung")
        String urlSlung,
        @JsonProperty("reading_passage_id_part1")
        ReadingPassageResponse readingPassageIdPart1,
        @JsonProperty("reading_passage_id_part2")
        ReadingPassageResponse readingPassageIdPart2,
        @JsonProperty("reading_passage_id_part3")
        ReadingPassageResponse readingPassageIdPart3
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
