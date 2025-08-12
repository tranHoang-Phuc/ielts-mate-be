package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record QuestionTypeStatsWrong(
        @JsonProperty("question_type")
        Integer questionType,
        @JsonProperty("wrong_count")
        long wrongCount,
        @JsonProperty("fill")
        String color
) {
}
