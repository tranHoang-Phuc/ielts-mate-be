package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamAttemptGetDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

public interface ExamAttemptService {
    @Transactional
    CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request);

    @Transactional(readOnly = true)
    ExamAttemptGetDetail getExamAttemptById(String examAttemptId, HttpServletRequest request) throws JsonProcessingException;
}
