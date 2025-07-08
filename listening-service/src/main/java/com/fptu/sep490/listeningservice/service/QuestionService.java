package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionService{
    @Transactional
    List<QuestionCreationResponse> createQuestions(
            List<QuestionCreationRequest> questionCreationRequest, HttpServletRequest request) throws JsonProcessingException;
}
