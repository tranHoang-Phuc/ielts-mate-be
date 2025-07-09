package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.UpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UpdatedQuestionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionService{
    List<QuestionCreationResponse> createQuestions(
            List<QuestionCreationRequest> questionCreationRequest, HttpServletRequest request) throws JsonProcessingException;

    UpdatedQuestionResponse updateQuestion(String questionId, UpdatedQuestionRequest questionCreationRequest, HttpServletRequest request);

    UpdatedQuestionResponse updateOrder(
            String questionId,
            String groupId,
            OrderUpdatedQuestionRequest questionCreationRequest,
            HttpServletRequest request
    ) throws JsonProcessingException;

    UpdatedQuestionResponse updateInformation(String questionId, String groupId,
                                              InformationUpdatedQuestionRequest informationRequest,
                                              HttpServletRequest request) throws JsonProcessingException;

    void deleteQuestion(String questionId, String groupId, HttpServletRequest request);
}
