package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ChoiceRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ChoiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface ChoiceService {
    ChoiceResponse createChoice(String questionId, @Valid ChoiceRequest choiceRequest, HttpServletRequest request) throws Exception;

    ChoiceResponse updateChoice(String questionId, String choiceId, @Valid ChoiceRequest choiceRequest, HttpServletRequest request) throws Exception;

    ChoiceResponse getChoiceById(String questionId, String choiceId, HttpServletRequest request);

    List<ChoiceResponse> getAllChoicesOfQuestion(String questionId, HttpServletRequest request);

    void deleteChoice(String questionId, String choiceId, HttpServletRequest request);

    void switchChoicesOrder(String questionId, String choiceId1, String choiceId2, HttpServletRequest request);
}
