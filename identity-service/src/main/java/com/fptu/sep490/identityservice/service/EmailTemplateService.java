package com.fptu.sep490.identityservice.service;

public interface EmailTemplateService {
    String buildVerificationEmail(String otp);

    String buildForgotPasswordEmail(String url);

    String buildEmailVerificationSuccess(String email, String fullName);

    String buildCustomEmail(String email, String s, String moduleName);
}
