package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.NotFoundException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.exception.ErrorNormalizer;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.viewmodel.LoginRequest;
import com.fptu.sep490.identityservice.viewmodel.UserAccessInfo;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    AuthServiceImpl authService;

    @Mock
    KeyCloakTokenClient keyCloakTokenClient;

    @Mock
    KeyCloakUserClient keyCloakUserClient;

    @Mock
    RedisService redisService;

    @Mock
    ErrorNormalizer errorNormalizer;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "realm", "test-realm");
        ReflectionTestUtils.setField(authService, "clientId", "test-client");
        ReflectionTestUtils.setField(authService, "clientSecret", "test-secret");
    }

    @Test
    void login_success() throws Exception {
        String username = "testuser@example.com";
        String password = "password";

        // Token đã cache sẵn
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-token");

        // User có trong Keycloak
        UserAccessInfo user = new UserAccessInfo(
                "id", "testuser", "First", "Last", username,
                true, 0L, true, false, null
        );
        Mockito.when(keyCloakUserClient.getUserByEmail(
                eq("test-realm"),
                eq("Bearer cached-token"),
                eq(username)
        )).thenReturn(List.of(user));

        // Lấy token thành công từ Keycloak
        KeyCloakTokenResponse tokenResponse = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .expiresIn(3600)
                .build();
        Mockito.when(keyCloakTokenClient.requestToken(
                Mockito.<MultiValueMap<String, String>>argThat(map ->
                        map.getFirst("grant_type").equals("password") &&
                                map.getFirst("username").equals("testuser@example.com") &&
                                map.getFirst("password").equals("password") &&
                                map.getFirst("client_id").equals("test-client") &&
                                map.getFirst("client_secret").equals("test-secret")
                ),
                eq("test-realm")
        )).thenReturn(tokenResponse);
        KeyCloakTokenResponse result = authService.login(username, password);

        assertEquals("access-token", result.accessToken());
    }

    @Test
    void login_userNotFound_throwsNotFoundException() throws Exception {
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-token");

        Mockito.when(keyCloakUserClient.getUserByEmail(
                        anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () ->
                authService.login("unknown@example.com", "pass"));
    }

    @Test
    void login_emailNotVerified_throwsAppException() throws Exception {
        String username = "unverified@example.com";

        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-token");

        UserAccessInfo user = new UserAccessInfo(
                "id", "user", "first", "last", username, false,
                0L, true, false, null
        );

        Mockito.when(keyCloakUserClient.getUserByEmail(
                anyString(), anyString(), anyString())).thenReturn(List.of(user));

        AppException ex = assertThrows(AppException.class, () ->
                authService.login(username, "pass"));

        assertEquals(Constants.ErrorCode.EMAIL_NOT_VERIFIED, "00028");
    }

    @Test
    void getCachedClientToken_cacheHit_returnsCachedToken() throws Exception {
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-token");

        String result = authService.getCachedClientToken();

        assertEquals("cached-token", result);
    }

    @Test
    void getCachedClientToken_cacheMiss_fetchAndSaveToRedis() throws Exception {
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn(null);

        KeyCloakTokenResponse response = KeyCloakTokenResponse.builder()
                .accessToken("new-token")
                .expiresIn(1800)
                .build();

        MultiValueMap<String, String> expectedParams = new LinkedMultiValueMap<>();
        expectedParams.add("grant_type", "client_credentials");
        expectedParams.add("client_id", "test-client");
        expectedParams.add("client_secret", "test-secret");
        expectedParams.add("scope", "openid");

        Mockito.when(keyCloakTokenClient.requestToken(
                Mockito.<MultiValueMap<String, String>>argThat(map ->
                        "client_credentials".equals(map.getFirst("grant_type")) &&
                                "test-client".equals(map.getFirst("client_id")) &&
                                "test-secret".equals(map.getFirst("client_secret")) &&
                                "openid".equals(map.getFirst("scope"))
                ),
                eq("test-realm")
        )).thenReturn(response);

        String result = authService.getCachedClientToken();

        assertEquals("new-token", result);
        Mockito.verify(redisService)
                .saveValue(anyString(), eq(response.accessToken()) , Mockito.any());
    }
}
