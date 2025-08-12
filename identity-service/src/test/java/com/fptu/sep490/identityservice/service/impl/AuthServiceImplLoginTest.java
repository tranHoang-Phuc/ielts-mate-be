package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.NotFoundException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.viewmodel.UserAccessInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private RedisService redisService;

    @Mock
    private KeyCloakTokenClient keyCloakTokenClient;

    @Mock
    private KeyCloakUserClient keyCloakUserClient;

    private final String realm = "test-realm";
    private final String clientId = "test-client-id";
    private final String clientSecret = "test-client-secret";


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "realm", realm);
        ReflectionTestUtils.setField(authService, "clientId", clientId);
        ReflectionTestUtils.setField(authService, "clientSecret", clientSecret);
    }

    @Test
    void login_success() throws Exception {
        // Given
        String username = "test@example.com";
        String password = "password";

        // 1. Mock cached client token
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-client-token");

        // 2. Mock Keycloak user info (đủ 10 field)
        UserAccessInfo.AccessInfo accessInfo = new UserAccessInfo.AccessInfo(
                true, true, true, true, true
        );
        UserAccessInfo userInfo = new UserAccessInfo(
                "id-123",
                username,
                "First",
                "Last",
                username,
                true,
                System.currentTimeMillis(),
                true,
                false,
                accessInfo
        );
        Mockito.when(keyCloakUserClient.getUserByEmail(eq(realm), anyString(), eq(username)))
                .thenReturn(List.of(userInfo));

        // 3. Mock token response (đủ 8 field)
        KeyCloakTokenResponse expectedToken = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .notBeforePolicy("0")
                .sessionState("session-123")
                .scope("openid profile email")
                .build();
        Mockito.when(
                keyCloakTokenClient.requestToken(
                        Mockito.<MultiValueMap<String, String>>any(),
                        Mockito.eq(realm)
                )
        ).thenReturn(expectedToken);

        // When
        KeyCloakTokenResponse actual = authService.login(username, password);

        // Then
        assertNotNull(actual);
        assertEquals("access-token", actual.accessToken());
        assertEquals("refresh-token", actual.refreshToken());

        Mockito.verify(redisService).getValue(contains("client-token"), eq(String.class));
        Mockito.verify(keyCloakUserClient).getUserByEmail(eq(realm), anyString(), eq(username));
        Mockito.verify(keyCloakTokenClient)
                .requestToken(Mockito.any(MultiValueMap.class), Mockito.eq(realm));
    }

    @Test
    void login_userNotFound_throwsNotFoundException() throws JsonProcessingException {
        // Given
        String username = "notfound@example.com";

        // Mock Redis có client token
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-client-token");

        // Mock Keycloak trả về list rỗng -> user không tồn tại
        Mockito.when(keyCloakUserClient.getUserByEmail(eq(realm), anyString(), eq(username)))
                .thenReturn(List.of());

        // Then
        assertThrows(NotFoundException.class,
                () -> authService.login(username, "password"));
    }

    @Test
    void login_firstElementNull_throwsNotFoundException() throws JsonProcessingException {
        String username = "nulluser@example.com";

        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-client-token");

        // Trả về list chứa 1 phần tử null
        List<UserAccessInfo> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        Mockito.when(keyCloakUserClient.getUserByEmail(eq(realm), anyString(), eq(username)))
                .thenReturn(listWithNull);

        assertThrows(NotFoundException.class,
                () -> authService.login(username, "password"));
    }


    @Test
    void login_noCachedTokenAndKeycloakFails_throwsRuntimeException() throws JsonProcessingException {
        String username = "test@example.com";

        // No cached token
        Mockito.when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn(null);

        // Khi getCachedClientToken gọi keyCloakTokenClient.requestToken(...) => ném lỗi
        Mockito.when(keyCloakTokenClient.requestToken(
                Mockito.<MultiValueMap<String, String>>any(),
                Mockito.eq(realm)
        )).thenThrow(new RuntimeException("Keycloak down"));

        // Expect the RuntimeException (nguyên gốc) to be propagated
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(username, "password"));
        assertEquals("Keycloak down", ex.getMessage());
    }


    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void login_cacheFails_butStillReturnsToken() throws Exception {
        String username = "test@example.com";

        // Cho phép mock này có thể không được gọi
        lenient().when(redisService.getValue(anyString(), eq(String.class)))
                .thenReturn("cached-client-token");

        UserAccessInfo userInfo = new UserAccessInfo(
                "id",
                username,
                "First",
                "Last",
                username + "@mail.com",
                true,
                System.currentTimeMillis(),
                true,
                false,
                new UserAccessInfo.AccessInfo(
                        true, true, true, false, true
                )
        );

        // Cho phép mock này có thể không được gọi
       when(keyCloakUserClient.getUserByEmail(eq(realm), anyString(), eq(username)))
                .thenReturn(List.of(userInfo));

        KeyCloakTokenResponse expectedToken = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .scope("openid")
                .build();

        // Cho phép mock này có thể không được gọi
        lenient().when(keyCloakTokenClient.requestToken(
                        Mockito.<MultiValueMap<String, String>>any(),
                        Mockito.eq(realm)))
                .thenReturn(expectedToken);

        // Giả lập lưu cache thất bại
        Mockito.doThrow(new RuntimeException("Redis fail"))
                .when(redisService)
                .saveValue(anyString(), anyString(), ArgumentMatchers.any(Duration.class));

        KeyCloakTokenResponse actual = authService.login(username, "password");

        assertEquals("access-token", actual.accessToken());
    }

    @Test
    void refreshToken_success() {
        // Given
        String refreshToken = "sample-refresh-token";
        KeyCloakTokenResponse expectedResponse = KeyCloakTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .tokenType("Bearer")
                .scope("openid")
                .build();

        // Giả lập keyCloakTokenClient trả về token mới
        when(keyCloakTokenClient.requestToken(
                Mockito.<MultiValueMap<String, String>>any(),
                Mockito.eq(realm)))
                .thenReturn(expectedResponse);

        // When
        KeyCloakTokenResponse actualResponse = authService.refreshToken(refreshToken);

        // Then
        assertNotNull(actualResponse);
        assertEquals("new-access-token", actualResponse.accessToken());
        assertEquals("new-refresh-token", actualResponse.refreshToken());
        assertEquals(3600, actualResponse.expiresIn());
        assertEquals(7200, actualResponse.refreshExpiresIn());
        assertEquals("Bearer", actualResponse.tokenType());
        assertEquals("openid", actualResponse.scope());

        // Verify form được build đúng
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient).requestToken(formCaptor.capture(), eq(realm));

        MultiValueMap<String, String> capturedForm = formCaptor.getValue();
        assertEquals("refresh_token", capturedForm.getFirst("grant_type"));
        assertEquals(clientId, capturedForm.getFirst("client_id"));
        assertEquals(clientSecret, capturedForm.getFirst("client_secret"));
        assertEquals(refreshToken, capturedForm.getFirst("refresh_token"));
    }
    @Test
    void logout_success_callsKeycloakWithCorrectParams() {
        // GIVEN
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-456";

        // WHEN
        authService.logout(accessToken, refreshToken);

        // THEN: verify đúng param gọi tới client
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient, times(1))
                .logout(eq(realm), formCaptor.capture(), eq("Bearer " + accessToken));

        MultiValueMap<String, String> formSent = formCaptor.getValue();
        assertThat(formSent.getFirst("client_id")).isEqualTo(clientId);
        assertThat(formSent.getFirst("client_secret")).isEqualTo(clientSecret);
        assertThat(formSent.getFirst("refresh_token")).isEqualTo(refreshToken);
    }

    @Test
    void introspect_success_returnsResponse() {
        // GIVEN
        String accessToken = "access-token-xyz";
        IntrospectResponse mockResponse = IntrospectResponse.builder()
                .active(true)
                .username("testuser")
                .build();

        when(keyCloakTokenClient.introspect(eq(realm), Mockito.<MultiValueMap<String, String>>any()))
                .thenReturn(mockResponse);

        // WHEN
        IntrospectResponse actual = authService.introspect(accessToken);

        // THEN: verify gọi đúng param
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(keyCloakTokenClient, times(1))
                .introspect(eq(realm), formCaptor.capture());

        MultiValueMap<String, String> formSent = formCaptor.getValue();
        assertThat(formSent.getFirst("client_id")).isEqualTo(clientId);
        assertThat(formSent.getFirst("client_secret")).isEqualTo(clientSecret);
        assertThat(formSent.getFirst("token")).isEqualTo(accessToken);

        // Kiểm tra kết quả trả về
        assertThat(actual).isNotNull();
        assertThat(actual.isActive()).isTrue();
        assertThat(actual.getUsername()).isEqualTo("testuser");
    }
}


