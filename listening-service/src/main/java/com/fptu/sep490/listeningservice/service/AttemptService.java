package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.SubmittedAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserDataAttempt;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface AttemptService {
    AttemptResponse createAttempt(UUID listeningTaskId, HttpServletRequest request) throws JsonProcessingException;

    void saveAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers);

    UserDataAttempt loadAttempt(String attemptId, HttpServletRequest request) throws JsonProcessingException;

    SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) throws JsonProcessingException;
}
