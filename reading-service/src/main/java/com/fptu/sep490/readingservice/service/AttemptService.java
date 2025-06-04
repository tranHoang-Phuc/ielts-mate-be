package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.response.PassageAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AttemptService {
    PassageAttemptResponse createAttempt(String passageId, HttpServletRequest request) throws JsonProcessingException;
}
