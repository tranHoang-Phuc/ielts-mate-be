package com.fptu.sep490.identityservice.service.impl;

import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    KeyCloakTokenClient keyCloakTokenClient;

    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;



    @Override
    public KeyCloakTokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid");
        return keyCloakTokenClient.requestToken(form, realm);
    }

    @Override
    public KeyCloakTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String , String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        return keyCloakTokenClient.requestToken(form, realm);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        MultiValueMap<String , String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        keyCloakTokenClient.logout(realm, form, "Bearer " + accessToken);
    }

    @Override
    public IntrospectResponse introspect(String accessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("token", accessToken);
        return keyCloakTokenClient.introspect(realm, form);
    }
}
