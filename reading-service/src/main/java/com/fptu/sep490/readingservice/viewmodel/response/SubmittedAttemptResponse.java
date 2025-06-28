package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmittedAttemptResponse {

    @JsonProperty("duration")
     Long duration;

    @JsonProperty("result_sets")
    List<ResultSet> resultSets;

    // Jackson sẽ dùng builder này để deserialize


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResultSet {

        @JsonProperty("question_index")
         int questionIndex;

        @JsonProperty("correct_answer")
         List<String>correctAnswer;

        @JsonProperty("user_answer")
         List<String> userAnswer;

        @JsonProperty("is_correct")
         boolean isCorrect;

        @JsonProperty("explanation")
         String explanation;


    }
}
