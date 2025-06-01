package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PassageService {
    PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest, HttpServletRequest request) throws JsonProcessingException;
}
