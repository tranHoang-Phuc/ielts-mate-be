package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.exception.ErrorNormalizer;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.UserCreationParam;
import com.fptu.sep490.identityservice.viewmodel.UserCreationRequest;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    KeyCloakTokenClient keyCloakTokenClient;
    ErrorNormalizer errorNormalizer;
    RedisService redisService;
    KeyCloakUserClient keyCloakUserClient;

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
    public KeyCloakTokenResponse login(String username, String password) throws JsonProcessingException {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("username", username);
            form.add("password", password);
            form.add("scope", "openid");
            return keyCloakTokenClient.requestToken(form, realm);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
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

    @Override
    public String createUser(UserCreationRequest request) throws JsonProcessingException {
        String clientToken = getCachedClientToken();
        UserCreationParam userCreationParam = UserCreationParam.builder()
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .emailVerified(true)
                .credentials(
                        List.of(
                                UserCreationParam.Credential.builder()
                                        .type("password")
                                        .value(request.password())
                                        .temporary(false)
                                        .build()
                        )
                )
                .build();
        try {
            var creationResponse = keyCloakUserClient.createUser(realm, "Bearer " + clientToken, userCreationParam);
            return extractUserId(creationResponse);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }

    }

    private String extractUserId(ResponseEntity<?> response){
        String location = response.getHeaders().get("Location").getFirst();
        String[] splitedStr = location.split("/");
        return splitedStr[splitedStr.length - 1];
    }


    private String getCachedClientToken() {
        final String cacheKey = Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN;

        String cachedToken = redisService.get(cacheKey, String.class);
        if (cachedToken != null) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "openid");

        KeyCloakTokenResponse tokenResponse = keyCloakTokenClient.requestToken(form, realm);
        String newToken = tokenResponse.accessToken();
        var expiresIn = tokenResponse.expiresIn();
        redisService.save(cacheKey, newToken, Duration.ofSeconds(expiresIn));
        return newToken;
    }
}
