package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmittedAttemptResponse(
        @JsonProperty("duration")
        Integer duration,
        @JsonProperty("result_sets")
        List<ResultSet> resultSets
) {
    public record ResultSet(
            @JsonProperty("question_index")
            int questionIndex,
            @JsonProperty("correct_answer")
            String correctAnswer,
            @JsonProperty("user_answer")
            String userAnswer,
            @JsonProperty("is_correct")
            boolean isCorrect,
            @JsonProperty("explanation")
            String explanation
    ) {
    }
}
