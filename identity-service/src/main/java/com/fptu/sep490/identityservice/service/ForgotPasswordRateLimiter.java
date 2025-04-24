package com.fptu.sep490.identityservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface ForgotPasswordRateLimiter {
    boolean isBlocked(String email) throws JsonProcessingException;

    void recordAttempt(String email) throws JsonProcessingException;
}
