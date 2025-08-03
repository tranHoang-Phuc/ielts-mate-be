package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.KeyCloakRuntimeException;
import com.fptu.sep490.commonlibrary.exceptions.NotFoundException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.component.AesSecretKey;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.exception.ErrorNormalizer;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.service.EmailTemplateService;
import com.fptu.sep490.identityservice.service.ForgotPasswordRateLimiter;
import com.fptu.sep490.identityservice.service.VerifyEmailRateLimiter;
import com.fptu.sep490.identityservice.viewmodel.UserAccessInfo;
import com.fptu.sep490.identityservice.viewmodel.UserCreationProfile;
import com.fptu.sep490.identityservice.viewmodel.UserCreationRequest;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private KeyCloakTokenClient keyCloakTokenClient;
    @Mock
    private KeyCloakUserClient keyCloakUserClient;
    @Mock
    private ErrorNormalizer errorNormalizer;
    @Mock
    private RedisService redisService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    @Mock
    private VerifyEmailRateLimiter verifyEmailRateLimiter;
    @Mock
    private AesSecretKey aesSecretKey;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "kcUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(authService, "realm", "test-realm");
        ReflectionTestUtils.setField(authService, "clientId", "test-client");
        ReflectionTestUtils.setField(authService, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(authService, "emailVerifySecret", "email-secret");
        ReflectionTestUtils.setField(authService, "emailVerifyTokenExpireTime", 3600);
        ReflectionTestUtils.setField(authService, "userVerificationTopic", "user-verification");
        ReflectionTestUtils.setField(authService, "issuerUri", "http://localhost:8080/realms/test-realm");
        ReflectionTestUtils.setField(authService, "clientDomain", "http://localhost:3000");
        ReflectionTestUtils.setField(authService, "redirectUri", "http://localhost:3000/callback");
    }

    // todo: Login
    // Đăng nhập thành công
    @Test
    void login_success() throws Exception {
        // Given
        String username = "test@example.com";
        String password = "secret";
        String token = "client_token";

        UserAccessInfo.AccessInfo accessInfo = new UserAccessInfo.AccessInfo(true, true, true, true, true);
        UserAccessInfo user = new UserAccessInfo(
                "1", username, "First", "Last", username, true,
                System.currentTimeMillis(), true, false, accessInfo
        );

        KeyCloakTokenResponse response = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600)
                .build();

        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn(token).when(spyService).getCachedClientToken();
        when(keyCloakUserClient.getUserByEmail("test-realm", "Bearer " + token, username))
                .thenReturn(List.of(user));
        when(keyCloakTokenClient.requestToken(any(), eq("test-realm")))
                .thenReturn(response);

        // When
        KeyCloakTokenResponse result = spyService.login(username, password);

        // Then
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(keyCloakTokenClient).requestToken(any(), eq("test-realm"));
    }

    // user khong ton tai
    @Test
    void login_userNotFound_shouldThrowNotFoundException() throws Exception {
        String username = "notfound@example.com";
        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn("token").when(spyService).getCachedClientToken();

        when(keyCloakUserClient.getUserByEmail(any(), any(), eq(username)))
                .thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> spyService.login(username, "pass"));
    }

    // user ton tai nhung email chua duoc xac thuc
    @Test
    void login_emailNotVerified_shouldThrowAppException() throws Exception {
        String username = "unverified@example.com";
        UserAccessInfo user = new UserAccessInfo(
                "1", username, "First", "Last", username, false,
                System.currentTimeMillis(), true, false, null
        );

        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn("token").when(spyService).getCachedClientToken();
        when(keyCloakUserClient.getUserByEmail(any(), any(), any()))
                .thenReturn(List.of(user));

        AppException ex = assertThrows(AppException.class, () -> spyService.login(username, "pass"));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.EMAIL_NOT_VERIFIED), "Email is not verified");
    }

    // user ton tai nhung tai khoan bi khoa
    @Test
    void login_wrongPassword_shouldThrowAppException() throws Exception {
        String username = "wrongpass@example.com";
        UserAccessInfo user = new UserAccessInfo(
                "1", username, "First", "Last", username, true,
                System.currentTimeMillis(), true, false, null
        );

        FeignException unauthorizedEx = mock(FeignException.Unauthorized.class);
        AppException expectedEx = new AppException(Constants.ErrorCodeMessage.WRONG_PASSWORD, "ERR", 401);

        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn("token").when(spyService).getCachedClientToken();

        when(keyCloakUserClient.getUserByEmail(any(), any(), any()))
                .thenReturn(List.of(user));
        when(keyCloakTokenClient.requestToken(any(), any()))
                .thenThrow(unauthorizedEx);
        when(errorNormalizer.handleKeyCloakException(unauthorizedEx)).thenThrow(expectedEx);

        AppException thrown = assertThrows(AppException.class, () -> spyService.login(username, "pass"));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.WRONG_PASSWORD), thrown.getMessage());
    }

    // user ton tai nhung co loi khi lay token tu Keycloak
    @Test
    void login_keycloakError_shouldThrowKeycloakRuntimeException() throws Exception {
        String username = "user@example.com";
        UserAccessInfo user = new UserAccessInfo(
                "1", username, "First", "Last", username, true,
                System.currentTimeMillis(), true, false, null
        );
        FeignException genericEx = mock(FeignException.class);
        KeyCloakRuntimeException expectedEx = new KeyCloakRuntimeException("KEYCLOAK_ERROR", "KEYCLOAK_ERROR");

        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn("token").when(spyService).getCachedClientToken();

        when(keyCloakUserClient.getUserByEmail(any(), any(), any()))
                .thenReturn(List.of(user));
        when(keyCloakTokenClient.requestToken(any(), any()))
                .thenThrow(genericEx);
        when(errorNormalizer.handleKeyCloakException(genericEx))
                .thenReturn(expectedEx);

        KeyCloakRuntimeException thrown = assertThrows(KeyCloakRuntimeException.class,
                () -> spyService.login(username, "pass"));
        assertEquals(MessagesUtils.getMessage("KEYCLOAK_ERROR"), thrown.getMessage());
    }

    @Test
    void refreshToken_success_shouldReturnTokenResponse() {
        // Given
        String inputRefreshToken = "dummy-refresh-token";
        KeyCloakTokenResponse expectedResponse = KeyCloakTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(3600)
                .build();

        // When
        when(keyCloakTokenClient.requestToken(any(), eq("test-realm"))).thenReturn(expectedResponse);

        // Then
        KeyCloakTokenResponse actualResponse = authService.refreshToken(inputRefreshToken);

        assertEquals("new-access-token", actualResponse.accessToken());
        assertEquals("new-refresh-token", actualResponse.refreshToken());

        // Verify request body sent to KeyCloak
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient).requestToken(formCaptor.capture(), eq("test-realm"));

        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("refresh_token", form.getFirst("grant_type"));
        assertEquals("test-client", form.getFirst("client_id"));
        assertEquals("test-secret", form.getFirst("client_secret"));
        assertEquals("dummy-refresh-token", form.getFirst("refresh_token"));
    }

    @Test
    void logout_shouldCallKeycloakLogoutWithCorrectParams() {
        // Given
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        // When
        authService.logout(accessToken, refreshToken);

        // Then
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient).logout(eq("test-realm"), formCaptor.capture(), eq("Bearer access-token"));

        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("test-client", form.getFirst("client_id"));
        assertEquals("test-secret", form.getFirst("client_secret"));
        assertEquals("refresh-token", form.getFirst("refresh_token"));
    }

    @Test
    void introspect_shouldReturnIntrospectResponseFromKeycloak() {
        // Given
        String accessToken = "access-token";
        IntrospectResponse expectedResponse = IntrospectResponse.builder()
                .active(true)
                .username("testuser")
                .build();

        // Mock behavior
        when(keyCloakTokenClient.introspect(anyString(), any(MultiValueMap.class)))
                .thenReturn(expectedResponse);

        // When
        IntrospectResponse actualResponse = authService.introspect(accessToken);

        // Then
        assertNotNull(actualResponse);
        assertTrue((actualResponse).isActive());
        assertEquals("testuser", actualResponse.getUsername());

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient).introspect(eq("test-realm"), formCaptor.capture());

        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("test-client", form.getFirst("client_id"));
        assertEquals("test-secret", form.getFirst("client_secret"));
        assertEquals(accessToken, form.getFirst("token"));
    }
    @Test
    void createUser_success() throws Exception {
        // Given
        UserCreationRequest request = UserCreationRequest.builder()
                .email("test@example.com")
                .firstName("First")
                .lastName("Last")
                .password("password123")
                .build();

        AuthServiceImpl spyService = Mockito.spy(authService);
        doReturn("cached-client-token").when(spyService).getCachedClientToken();

        // Ép kiểu rõ ràng để tránh lỗi wildcard
        ResponseEntity<?> mockResponse = ResponseEntity
                .status(201)
                .header("Location", "http://localhost:8080/admin/realms/test-realm/users/abc123")
                .build();

        doReturn("abc123").when(spyService).extractUserId(mockResponse);

//        when(keyCloakUserClient.createUser(eq("test-realm"), eq("Bearer cached-client-token"), any()))
//                .thenReturn(mockResponse);
        when(aesSecretKey.encrypt("password123")).thenReturn("encrypted-password");

        // When
        UserCreationProfile result = spyService.createUser(request);

        // Then
        assertNotNull(result);
        assertEquals("abc123", result.id());
        assertEquals("test@example.com", result.email());
        assertEquals("First", result.firstName());
        assertEquals("Last", result.lastName());

        verify(redisService).saveValue(
                eq(spyService.getPasswordKey("test@example.com")),
                eq("encrypted-password")
        );
    }



//    @Test
//    void createUser_feignException_shouldThrowNormalizedError() throws Exception {
//        // Given
//        UserCreationRequest request = UserCreationRequest.builder()
//                .email("fail@example.com")
//                .firstName("First")
//                .lastName("Last")
//                .password("password123")
//                .build();
//
//        AuthServiceImpl spyService = Mockito.spy(authService);
//        doReturn("cached-client-token").when(spyService).getCachedClientToken();
//
//        FeignException feignException = mock(FeignException.class);
//        when(keyCloakUserClient.createUser(any(), any(), any())).thenThrow(feignException);
//
//        AppException normalizedException = new AppException("Normalized error", "", 500);
//        when(errorNormalizer.handleKeyCloakException(feignException)).thenReturn(normalizedException);
//
//        // When & Then
//        AppException ex = assertThrows(AppException.class, () -> spyService.createUser(request));
//        assertEquals("Normalized error", ex.getMessage());
//    }

}
