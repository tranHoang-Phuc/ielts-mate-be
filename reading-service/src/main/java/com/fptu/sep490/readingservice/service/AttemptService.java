package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AttemptService {
    AttemptResponse createAttempt(String passageId, HttpServletRequest request)
            throws JsonProcessingException;

    void saveAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers);

    UserDataAttempt loadAttempt(String attemptId, HttpServletRequest request) throws JsonProcessingException;

    SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers)
            throws JsonProcessingException;
}
