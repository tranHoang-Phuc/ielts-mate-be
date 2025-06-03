package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface QuestionService {
    List<QuestionCreationResponse> createQuestions(List<QuestionCreationRequest> questionCreationResponses, HttpServletRequest request) throws JsonProcessingException;
}
