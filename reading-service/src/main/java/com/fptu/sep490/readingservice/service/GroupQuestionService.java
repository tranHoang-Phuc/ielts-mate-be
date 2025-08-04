package com.fptu.sep490.readingservice.service;
import com.fptu.sep490.readingservice.viewmodel.request.AddGroupQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface GroupQuestionService {
    AddGroupQuestionResponse createGroupQuestion(String passageId, AddGroupQuestionRequest request, HttpServletRequest httpServletRequest) throws Exception;

    List<AddGroupQuestionResponse> getAllQuestionsGroupsOfPassages(String passageId, HttpServletRequest request) throws Exception;

    AddGroupQuestionResponse updateGroupQuestion(String passageId, String groupId, AddGroupQuestionRequest request, HttpServletRequest httpServletRequest) throws Exception;

    void deleteGroupQuestion(String passageId, String groupId, HttpServletRequest httpServletRequest) throws Exception;
 }
