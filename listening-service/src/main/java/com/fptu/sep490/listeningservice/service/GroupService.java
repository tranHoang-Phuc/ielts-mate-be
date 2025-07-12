package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface GroupService {
    QuestionGroupResponse createGroup(String listeningTaskId, QuestionGroupCreationRequest request, HttpServletRequest httpServletRequest) throws Exception;

    void deleteGroup(UUID listeningTaskId, UUID groupId, HttpServletRequest httpServletRequest) throws Exception;
}
