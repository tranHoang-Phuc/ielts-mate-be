package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

public interface ExamAttemptService {
    @Transactional
    CreateExamAttemptResponse createExamAttempt (String urlSlug, HttpServletRequest request) throws JsonProcessingException;
}
