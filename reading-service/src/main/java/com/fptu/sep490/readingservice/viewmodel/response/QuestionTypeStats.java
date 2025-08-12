package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record QuestionTypeStats(
        @JsonProperty("question_type")
        Integer questionType,
        @JsonProperty("correct_count")
        Long correctCount,
        @JsonProperty("fill")
        String color
) {
}
