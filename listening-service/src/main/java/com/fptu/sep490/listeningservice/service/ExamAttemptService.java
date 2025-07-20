package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

public interface ExamAttemptService {
    @Transactional
    CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request);
}
