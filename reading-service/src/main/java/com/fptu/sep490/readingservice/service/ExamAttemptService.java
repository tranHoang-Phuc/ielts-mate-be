package com.fptu.sep490.readingservice.service;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.SubmittedAttemptResponse;

public interface ExamAttemptService {
    SubmittedAttemptResponse submittedExam(String attemptId, ExamAttemptAnswersRequest answers, HttpServletRequest request) throws JsonProcessingException;
}
