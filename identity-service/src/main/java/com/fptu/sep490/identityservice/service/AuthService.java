package com.fptu.sep490.identityservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface AuthService {
    KeyCloakTokenResponse login(String username, String password) throws JsonProcessingException;

    KeyCloakTokenResponse refreshToken(String refreshToken);

    void logout(String accessToken, String refreshToken);

    IntrospectResponse introspect(String accessToken);

    UserCreationProfile createUser(UserCreationRequest request) throws Exception;

    void sendVerifyEmail(String email) throws JsonProcessingException;

    UserAccessInfo getUserAccessInfo(String accessToken) throws JsonProcessingException;

    UserAccessInfo getUserAccessInfoByEmail(String email, String accessToken) throws JsonProcessingException;

    void resetPassword(ResetPasswordRequest resetPasswordRequest) throws JsonProcessingException;

    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws JsonProcessingException;

    KeyCloakTokenResponse verifyEmail(String email, String otp) throws Exception;

    void verifyResetToken(String email, String otp);

    String createGoogleUrl();

    UserProfileMappingRoles getUserProfile(String accessToken) throws JsonProcessingException;
    void checkResetPasswordToken(String email, String otp);

    void changePassword(String accessToken, @Valid PasswordChange changePasswordRequest) throws JsonProcessingException;

    UserCreationProfile updateUserProfile(String accessToken, @Valid UserUpdateRequest userUpdateRequest) throws JsonProcessingException;


    void sendEmail(SendEmailRequest sendEmailRequest) throws Exception;
}
