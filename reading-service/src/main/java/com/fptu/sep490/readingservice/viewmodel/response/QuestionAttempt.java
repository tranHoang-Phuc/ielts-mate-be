package com.fptu.sep490.readingservice.viewmodel.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttempt {
    int questionType;
    int numberOfCorrectAnswers;
    List<String> correctAnswer;

}
