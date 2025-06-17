package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreation;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedChoiceRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ChoiceService {

    List<QuestionCreationResponse.ChoiceResponse> getAllChoicesOfQuestion(String questionId);

    QuestionCreationResponse.ChoiceResponse
        createChoice(String questionId, ChoiceCreation choice, HttpServletRequest request) throws JsonProcessingException;

    QuestionCreationResponse.ChoiceResponse
        updateChoice(String questionId, String choiceId, UpdatedChoiceRequest choice, HttpServletRequest request);

    void deleteChoice(String questionId, String choiceId, HttpServletRequest request);
}
