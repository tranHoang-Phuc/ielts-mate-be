package com.fptu.sep490.identityservice.service;

public interface EmailTemplateService {
    String buildVerificationEmail(String otp);

    String buildForgotPasswordEmail(String url);
}
