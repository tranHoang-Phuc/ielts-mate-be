package com.fptu.sep490.readingservice.viewmodel.request;


import com.fasterxml.jackson.annotation.JsonProperty;
public record ReadingExamCreationRequest(
        @JsonProperty("reading_exam_name")
        String readingExamName,

        @JsonProperty("reading_exam_description")
        String readingExamDescription,

        @JsonProperty("url_slung")
        String urlSlung,

        @JsonProperty("reading_passage_id_part1")
        String readingPassageIdPart1,

        @JsonProperty("reading_passage_id_part2")
        String readingPassageIdPart2,

        @JsonProperty("reading_passage_id_part3")
        String readingPassageIdPart3
) {
}
