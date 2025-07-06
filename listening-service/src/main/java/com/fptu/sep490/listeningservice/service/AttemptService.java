package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.SubmittedAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserDataAttempt;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AttemptService {
    AttemptResponse createAttempt(UUID listeningTaskId, HttpServletRequest request) throws JsonProcessingException;

    void saveAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers);

    UserDataAttempt loadAttempt(String attemptId, HttpServletRequest request) throws JsonProcessingException;

    SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) throws JsonProcessingException;

    Page<UserAttemptResponse> getAttemptByUser(int i, int size, List<Integer> ieltsTypeList, List<Integer> statusList, List<Integer> partNumberList, String sortBy, String sortDirection, String title, UUID listeningTaskId, HttpServletRequest request);

    UserDataAttempt viewResult(UUID attemptId, HttpServletRequest request) throws JsonProcessingException;
}
