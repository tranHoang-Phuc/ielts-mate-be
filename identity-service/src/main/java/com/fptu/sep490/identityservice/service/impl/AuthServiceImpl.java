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
import com.fptu.sep490.identityservice.viewmodel.UserPendingVerify;
import com.fptu.sep490.identityservice.viewmodel.UserProfileResponse;
import com.fptu.sep490.identityservice.viewmodel.event.UserProfileEvent;
import feign.FeignException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    KeyCloakTokenClient keyCloakTokenClient;
    ErrorNormalizer errorNormalizer;
    RedisService redisService;
    KeyCloakUserClient keyCloakUserClient;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;

    @Value("${email.secret}")
    @NonFinal
    String emailVerifySecret;

    @Value("${email.expiration}")
    @NonFinal
    int emailVerifyTokenExpireTime;

    @Value("${kafka.topic.user-verification}")
    @NonFinal
    String userVerificationTopic;

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
                .emailVerified(false)
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
            var userId = extractUserId(creationResponse);
            UserPendingVerify userPendingVerify = new UserPendingVerify(userId, request.email());
            redisService.addToSet(Constants.RedisKey.USER_PENDING_VERIFY + request.email(), userPendingVerify);
            return userId;
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }

    }

    @Override
    public void sendVerifyEmail(String email) throws JsonProcessingException {
        String cacheKey = Constants.RedisKey.USER_PENDING_VERIFY + email;
        Set<UserPendingVerify> userPendingVerifies = redisService.getSet(cacheKey, UserPendingVerify.class);
        if (userPendingVerifies.isEmpty()) {
            return;
        }
        UserPendingVerify userPendingVerify = userPendingVerifies.iterator().next();
        String token = generateEmailVerifyToken(userPendingVerify.userId(), email);
        String clientToken = getCachedClientToken();
        UserProfileResponse userProfileResponse = keyCloakUserClient.getUserById(realm, "Bearer " + clientToken,
                userPendingVerify.userId());
        UserProfileEvent userProfileEvent = new UserProfileEvent(userProfileResponse, token,
                "Subject", "HtmlContent");
        kafkaTemplate.send(userVerificationTopic, userProfileEvent);
    }

    private String generateEmailVerifyToken(String userId, String email) {
        Key key = Keys.hmacShaKeyFor(emailVerifySecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + emailVerifyTokenExpireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String extractUserId(ResponseEntity<?> response){
        String location = response.getHeaders().get("Location").getFirst();
        String[] splitedStr = location.split("/");
        return splitedStr[splitedStr.length - 1];
    }


    private String getCachedClientToken() throws JsonProcessingException {
        final String cacheKey = Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN;

        String cachedToken = redisService.getValue(cacheKey, String.class);
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
        redisService.saveValue(cacheKey, newToken, Duration.ofSeconds(expiresIn));
        return newToken;
    }
}
