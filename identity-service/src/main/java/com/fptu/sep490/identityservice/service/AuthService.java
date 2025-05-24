package com.fptu.sep490.identityservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.*;

public interface AuthService {
    KeyCloakTokenResponse login(String username, String password) throws JsonProcessingException;

    KeyCloakTokenResponse refreshToken(String refreshToken);

    void logout(String accessToken, String refreshToken);

    IntrospectResponse introspect(String accessToken);

    UserCreationProfile createUser(UserCreationRequest request) throws JsonProcessingException;

    void sendVerifyEmail(String email) throws JsonProcessingException;

    UserAccessInfo getUserAccessInfo(String accessToken) throws JsonProcessingException;

    void resetPassword(ResetPasswordRequest resetPasswordRequest) throws JsonProcessingException;

    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws JsonProcessingException;

    void verifyEmail(String email, String otp) throws JsonProcessingException;

    void verifyResetToken(String email, String otp);

    String createGoogleUrl();

    UserCreationProfile getUserProfile(String accessToken) throws JsonProcessingException;
}
