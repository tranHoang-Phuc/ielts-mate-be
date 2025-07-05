package com.fptu.sep490.listeningservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface AttemptService {
    AttemptResponse createAttempt(UUID listeningTaskId, HttpServletRequest request) throws JsonProcessingException;
}
