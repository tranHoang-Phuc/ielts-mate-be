package com.fptu.sep490.identityservice.service;

import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.UserCreationRequest;

public interface AuthService {
    KeyCloakTokenResponse login(String username, String password);
    KeyCloakTokenResponse refreshToken(String refreshToken);
    void logout(String accessToken, String refreshToken);
    IntrospectResponse introspect(String accessToken);
    String createUser(UserCreationRequest request);
}
