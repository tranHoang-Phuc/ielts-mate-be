package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

public interface ExamAttemptService {

    CreateExamAttemptResponse createExamAttempt (String urlSlug, HttpServletRequest request) throws JsonProcessingException;

    Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String readingExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    );
}
