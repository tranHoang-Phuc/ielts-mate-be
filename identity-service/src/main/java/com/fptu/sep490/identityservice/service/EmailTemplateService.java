package com.fptu.sep490.identityservice.service;

public interface EmailTemplateService {
    String buildVerificationEmail(String url);

    String buildForgotPasswordEmail(String url);
}
