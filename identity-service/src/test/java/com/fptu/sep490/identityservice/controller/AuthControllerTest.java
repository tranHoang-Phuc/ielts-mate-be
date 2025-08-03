package com.fptu.sep490.identityservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.CookieConstants;
import com.fptu.sep490.commonlibrary.exceptions.AccessDeniedException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.identityservice.service.impl.AuthServiceImpl;
import com.fptu.sep490.identityservice.viewmodel.*;
import feign.FeignException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.OK;

public class AuthControllerTest {
    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    private final AuthController controller = new AuthController(authService); // Replace bằng controller bạn đang dùng

    @Test
    void signIn_ReturnsTokenResponse() throws JsonProcessingException {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("phucth115.dev@gmail.com")
                .password("12345678")
                .build();
        KeyCloakTokenResponse tokenResponse = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .notBeforePolicy("0")
                .sessionState("session-state")
                .scope("openid profile email")
                .build();
        when(authService.login(loginRequest.email(), loginRequest.password())).thenReturn(tokenResponse);

        ResponseEntity<BaseResponse<KeyCloakTokenResponse>> responseEntity =
                authController.signIn(loginRequest, httpServletResponse);

        assertNotNull(responseEntity);
        assertEquals(OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("access-token", responseEntity.getBody().data().accessToken());
        assertEquals(3600, responseEntity.getBody().data().expiresIn());
        assertEquals(7200, responseEntity.getBody().data().refreshExpiresIn());
        assertEquals("refresh-token", responseEntity.getBody().data().refreshToken());
        assertEquals("Bearer", responseEntity.getBody().data().tokenType());
        assertEquals("0", responseEntity.getBody().data().notBeforePolicy());
        assertEquals("session-state", responseEntity.getBody().data().sessionState());
        assertEquals("openid profile email", responseEntity.getBody().data().scope());
        verify(authService, times(1)).login(loginRequest.email(), loginRequest.password());
    }

    @Test
    void register_ReturnsUserCreationProfile() throws Exception {
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("phucth115.dev@gmail.com")
                .password("PcyTt11@")
                .build();

        UserCreationProfile userCreationProfile = UserCreationProfile.builder()
                .id("12345")
                .email("newuser@gmail.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(authService.createUser(userCreationRequest)).thenReturn(userCreationProfile);

        ResponseEntity<BaseResponse<UserCreationProfile>> responseEntity =
                authController.register(userCreationRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("newuser@gmail.com", responseEntity.getBody().data().email());
        assertEquals("John", responseEntity.getBody().data().firstName());
        assertEquals("Doe", responseEntity.getBody().data().lastName());
        assertEquals("12345", responseEntity.getBody().data().id());
        verify(authService, times(1)).createUser(userCreationRequest);
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnTokenResponse() {
        String mockRefreshToken = "valid-refresh-token";
        KeyCloakTokenResponse mockTokenResponse = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .notBeforePolicy("0")
                .sessionState("session-state")
                .scope("openid profile email")
                .build();

        try (MockedStatic<CookieUtils> mockedStatic = mockStatic(CookieUtils.class)) {
            mockedStatic.when(() -> CookieUtils.getCookieValue(httpServletRequest, CookieConstants.REFRESH_TOKEN))
                    .thenReturn(mockRefreshToken);

            when(authService.refreshToken(mockRefreshToken)).thenReturn(mockTokenResponse);

            ResponseEntity<BaseResponse<KeyCloakTokenResponse>> response =
                    authController.refreshToken(httpServletRequest, httpServletResponse);

            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            assertEquals("access-token", response.getBody().data().accessToken());
            verify(authService, times(1)).refreshToken(mockRefreshToken);
        }
    }

    @Test
    void refreshToken_MissingRefreshToken_ShouldThrowUnauthorizedException() {
        try (MockedStatic<CookieUtils> mockedStatic = mockStatic(CookieUtils.class)) {
            mockedStatic.when(() -> CookieUtils.getCookieValue(httpServletRequest, CookieConstants.REFRESH_TOKEN))
                    .thenReturn(null);

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> authController.refreshToken(httpServletRequest, httpServletResponse)
            );
            assertEquals(Constants.ErrorCode.UNAUTHORIZED, exception.getBusinessErrorCode());
            assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.UNAUTHORIZED), "Unauthorized");
            verify(authService, never()).refreshToken(any());
        }
    }

    @Test
    void logout_WithHeaderAccessTokenAndRefreshToken_ShouldSucceed() {
        String bearerToken = "Bearer abc.def.ghi";
        String refreshToken = "mock-refresh-token";

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);

        try (MockedStatic<CookieUtils> mockedStatic = mockStatic(CookieUtils.class)) {
            mockedStatic.when(() -> CookieUtils.getCookieValue(httpServletRequest, CookieConstants.REFRESH_TOKEN))
                    .thenReturn(refreshToken);

            mockedStatic.when(() -> CookieUtils.revokeTokenCookies(httpServletResponse)).thenCallRealMethod();

            ResponseEntity<?> response = authController.logout(httpServletRequest, httpServletResponse);

            assertEquals(200, response.getStatusCodeValue());
            BaseResponse<?> body = (BaseResponse<?>) response.getBody();
            assertEquals("Logout successful", body.message());

            verify(authService).logout("abc.def.ghi", refreshToken);
        }
    }

    @Test
    void logout_WithAccessTokenFromCookie_ShouldSucceed() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        Cookie[] cookies = new Cookie[] {
                new Cookie("Authorization", "xyz.123.token"),
                new Cookie("refresh_token", "mock-refresh-token")
        };
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        try (MockedStatic<CookieUtils> mockedStatic = mockStatic(CookieUtils.class)) {
            mockedStatic.when(() -> CookieUtils.getCookieValue(httpServletRequest, CookieConstants.REFRESH_TOKEN))
                    .thenReturn("mock-refresh-token");
            mockedStatic.when(() -> CookieUtils.revokeTokenCookies(httpServletResponse)).thenCallRealMethod();

            ResponseEntity<?> response = authController.logout(httpServletRequest, httpServletResponse);

            assertEquals(200, response.getStatusCodeValue());
            BaseResponse<?> body = (BaseResponse<?>) response.getBody();
            assertEquals("Logout successful", body.message());

            verify(authService).logout("xyz.123.token", "mock-refresh-token");
        }
    }

    @Test
    void logout_MissingToken_ShouldThrowAccessDeniedException() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> authController.logout(httpServletRequest, httpServletResponse));

        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.ACCESS_DENIED), "Access denied");
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION), "Sign in required");

        verify(authService, never()).logout(any(), any());
    }

    @Test
    void introspect_WithAccessTokenInHeader_ReturnsIntrospectResponse() {
        String bearerToken = "Bearer valid.token.header";

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);

        IntrospectResponse introspectResponse = IntrospectResponse.builder()
                .active(true)
                .username("testuser")
                .email("test@example.com")
                .realmAccess(new IntrospectResponse.RealmAccess(List.of("admin", "user")))
                .resourceAccess(Map.of("client-app", new IntrospectResponse.ResourceAccess(List.of("read", "write"))))
                .build();

        when(authService.introspect("valid.token.header")).thenReturn(introspectResponse);

        ResponseEntity<BaseResponse<IntrospectResponse>> responseEntity = authController.introspect(httpServletRequest, httpServletResponse);

        assertEquals(200, responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getBody());
        assertEquals("testuser", responseEntity.getBody().data().getUsername());
        assertTrue(responseEntity.getBody().data().isActive());
        assertEquals("test@example.com", responseEntity.getBody().data().getEmail());
        assertEquals(List.of("admin", "user"), responseEntity.getBody().data().getRealmAccess().getRoles());
        assertEquals(List.of("read", "write"), responseEntity.getBody().data().getResourceAccess().get("client-app").getRoles());

        verify(authService).introspect("valid.token.header");
    }

    @Test
    void introspect_WithAccessTokenInCookie_ReturnsIntrospectResponse() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        Cookie authCookie = new Cookie("Authorization", "valid.token.cookie");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{authCookie});

        IntrospectResponse introspectResponse = IntrospectResponse.builder()
                .active(true)
                .username("cookieuser")
                .build();

        when(authService.introspect("valid.token.cookie")).thenReturn(introspectResponse);

        ResponseEntity<BaseResponse<IntrospectResponse>> responseEntity = authController.introspect(httpServletRequest, httpServletResponse);

        assertEquals(200, responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getBody());
        assertEquals("cookieuser", responseEntity.getBody().data().getUsername());
        assertTrue(responseEntity.getBody().data().isActive());

        verify(authService).introspect("valid.token.cookie");
    }

    @Test
    void introspect_MissingAccessToken_ThrowsAccessDeniedException() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> authController.introspect(httpServletRequest, httpServletResponse));

        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION), "Sign in required");
        assertEquals(MessagesUtils.getMessage(com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage.ACCESS_DENIED), "000008");

        verify(authService, never()).introspect(anyString());
    }

    @Test
    void checkEmailVerificationStatus_WithBearerTokenHeader_ReturnsUserAccessInfo() throws Exception {
        String token = "valid-access-token";
        String bearerHeader = "Bearer " + token;

        UserAccessInfo.AccessInfo accessInfo = new UserAccessInfo.AccessInfo(true, true, false, false, true);
        UserAccessInfo userAccessInfo = new UserAccessInfo(
                "id123",
                "testuser",
                "Test",
                "User",
                "testuser@example.com",
                true,
                123456789L,
                true,
                false,
                accessInfo
        );

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerHeader);
        when(authService.getUserAccessInfo(token)).thenReturn(userAccessInfo);

        ResponseEntity<BaseResponse<UserAccessInfo>> response = authController.checkEmailVerificationStatus(httpServletRequest);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userAccessInfo, response.getBody().data());
        verify(authService, times(1)).getUserAccessInfo(token);
    }

    @Test
    void checkEmailVerificationStatus_WithTokenInCookie_ReturnsUserAccessInfo() throws Exception {
        String token = "token-from-cookie";

        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        Cookie authCookie = new Cookie("Authorization", token);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{authCookie});

        UserAccessInfo.AccessInfo accessInfo = new UserAccessInfo.AccessInfo(false, false, true, true, false);
        UserAccessInfo userAccessInfo = new UserAccessInfo(
                "id456",
                "cookieuser",
                "Cookie",
                "User",
                "cookieuser@example.com",
                false,
                987654321L,
                false,
                true,
                accessInfo
        );

        when(authService.getUserAccessInfo(token)).thenReturn(userAccessInfo);

        ResponseEntity<BaseResponse<UserAccessInfo>> response = authController.checkEmailVerificationStatus(httpServletRequest);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userAccessInfo, response.getBody().data());
        verify(authService, times(1)).getUserAccessInfo(token);
    }

    @Test
    void checkEmailVerificationStatus_MissingToken_ThrowsAccessDeniedException() throws JsonProcessingException {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> authController.checkEmailVerificationStatus(httpServletRequest));

        assertNotNull(exception);
        verify(authService, never()).getUserAccessInfo(any());
    }

    @Test
    void sendOtp_ShouldReturnSuccessResponse() throws Exception {
        // Arrange
        SendOtpRequest request = SendOtpRequest.builder()
                .email("user@example.com")
                .build();

        doNothing().when(authService).sendVerifyEmail(request.email());

        // Act
        ResponseEntity<?> response = authController.sendOtp(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        assertTrue(response.getBody() instanceof BaseResponse<?>);
        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
        assertNull(baseResponse.data());
        assertEquals("OTP sent successfully", baseResponse.message());

        // Verify rằng authService.sendVerifyEmail được gọi 1 lần với email đúng
        verify(authService, times(1)).sendVerifyEmail(request.email());
    }

    @Test
    void verifyEmail_ShouldReturnSuccessResponseAndSetCookies() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");

        KeyCloakTokenResponse tokenResponse = KeyCloakTokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .tokenType("Bearer")
                .notBeforePolicy("0")
                .sessionState("session-state")
                .scope("openid profile email")
                .build();

        when(authService.verifyEmail(request.email(), request.otp())).thenReturn(tokenResponse);

        // Mock static method CookieUtils.setTokenCookies
        try (MockedStatic<CookieUtils> mockedCookieUtils = mockStatic(CookieUtils.class)) {

            // Act
            ResponseEntity<BaseResponse<KeyCloakTokenResponse>> response =
                    authController.verifyEmail(request, httpServletResponse);

            // Assert
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals("Email verified successfully", response.getBody().message());
            assertEquals(tokenResponse, response.getBody().data());

            // Verify service method called once
            verify(authService, times(1)).verifyEmail(request.email(), request.otp());

            // Verify static method called once with correct arguments
            mockedCookieUtils.verify(() -> CookieUtils.setTokenCookies(httpServletResponse, tokenResponse), times(1));
        }
    }


    @Test
    void resetPassword_ShouldReturnSuccessResponse() throws Exception {
        // Arrange
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("user@example.com")
                .password("newPassword123")
                .confirmPassword("newPassword123")
                .token("reset-token")
                .build();

        doNothing().when(authService).resetPassword(request);

        // Act
        ResponseEntity<?> response = authController.resetPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertNotNull(body);
        assertEquals("Password reset successfully", body.message());
        assertNull(body.data());

        verify(authService, times(1)).resetPassword(request);
    }

    @Test
    void forgotPassword_ShouldReturnSuccessResponse() throws Exception {
        // Arrange
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("user@example.com")
                .build();

        doNothing().when(authService).forgotPassword(request);

        // Act
        ResponseEntity<?> response = authController.forgotPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertNotNull(body);
        assertEquals("Forgot password email sent successfully", body.message());
        assertNull(body.data());

        verify(authService, times(1)).forgotPassword(request);
    }

    @Test
    void verifyResetToken_ShouldReturnSuccessResponse() {
        // Arrange
        VerifyResetTokenRequest request = VerifyResetTokenRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        doNothing().when(authService).checkResetPasswordToken(request.email(), request.otp());

        // Act
        ResponseEntity<?> response = authController.verifyResetToken(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertNotNull(body);
        assertEquals("Reset token verified successfully", body.message());
        assertNull(body.data());

        verify(authService, times(1)).checkResetPasswordToken(request.email(), request.otp());
    }

    @Test
    void getProfile_WithBearerTokenInHeader_ShouldReturnUserProfile() throws JsonProcessingException {
        // Arrange
        String token = "valid-access-token";
        String bearerHeader = "Bearer " + token;

        UserProfileMappingRoles userProfile = UserProfileMappingRoles.builder()
                .id("user123")
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(List.of("ROLE_USER", "ROLE_ADMIN"))
                .build();

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerHeader);
        when(authService.getUserProfile(token)).thenReturn(userProfile);

        // Act
        ResponseEntity<BaseResponse<UserProfileMappingRoles>> response = authController.getProfile(httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(userProfile, response.getBody().data());

        verify(authService, times(1)).getUserProfile(token);
    }

    @Test
    void getProfile_WithTokenInCookie_ShouldReturnUserProfile() throws JsonProcessingException {
        // Arrange
        String token = "token-from-cookie";

        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        Cookie authCookie = new Cookie("Authorization", token);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{authCookie});

        UserProfileMappingRoles userProfile = UserProfileMappingRoles.builder()
                .id("user456")
                .email("cookieuser@example.com")
                .firstName("Cookie")
                .lastName("User")
                .roles(List.of("ROLE_USER"))
                .build();

        when(authService.getUserProfile(token)).thenReturn(userProfile);

        // Act
        ResponseEntity<BaseResponse<UserProfileMappingRoles>> response = authController.getProfile(httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(userProfile, response.getBody().data());

        verify(authService, times(1)).getUserProfile(token);
    }

    @Test
    void getProfile_MissingToken_ShouldThrowAccessDeniedException() throws JsonProcessingException {
        // Arrange
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authController.getProfile(httpServletRequest));

        assertNotNull(ex);
        verify(authService, never()).getUserProfile(anyString());
    }

    @Test
    void changePassword_WithBearerTokenInHeader_ShouldChangePasswordSuccessfully() throws Exception {
        // Arrange
        String token = "valid-access-token";
        String bearerHeader = "Bearer " + token;

        PasswordChange passwordChangeRequest = new PasswordChange(
                "oldPassword123",
                "newPassword123",
                "newPassword123"
        );

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerHeader);

        // Act
        ResponseEntity<BaseResponse<Void>> response = authController.changePassword(passwordChangeRequest, httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Password changed successfully", response.getBody().message());
        assertNull(response.getBody().data());
        verify(authService, times(1)).changePassword(token, passwordChangeRequest);
    }

    @Test
    void changePassword_WithTokenInCookie_ShouldChangePasswordSuccessfully() throws Exception {
        // Arrange
        String token = "token-from-cookie";

        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        Cookie authCookie = new Cookie("Authorization", token);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{authCookie});

        PasswordChange passwordChangeRequest = new PasswordChange(
                "oldPassword123",
                "newPassword123",
                "newPassword123"
        );

        // Act
        ResponseEntity<BaseResponse<Void>> response = authController.changePassword(passwordChangeRequest, httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Password changed successfully", response.getBody().message());
        assertNull(response.getBody().data());
        verify(authService, times(1)).changePassword(token, passwordChangeRequest);
    }

    @Test
    void changePassword_MissingToken_ShouldThrowAccessDeniedException() throws JsonProcessingException {
        // Arrange
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        PasswordChange passwordChangeRequest = new PasswordChange(
                "oldPassword123",
                "newPassword123",
                "newPassword123"
        );

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authController.changePassword(passwordChangeRequest, httpServletRequest));

        assertNotNull(ex);
        verify(authService, never()).changePassword(anyString(), any());
    }

    @Test
    void updateProfile_WithBearerTokenInHeader_ShouldReturnUpdatedProfile() throws Exception {
        // Arrange
        String token = "valid-access-token";
        String bearerHeader = "Bearer " + token;

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "NewFirstName",
                "NewLastName"
        );

        UserCreationProfile updatedProfile = UserCreationProfile.builder()
                .id("user123")
                .firstName(updateRequest.firstName())
                .lastName(updateRequest.lastName())
                .build();

        when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerHeader);
        when(authService.updateUserProfile(token, updateRequest)).thenReturn(updatedProfile);

        // Act
        ResponseEntity<BaseResponse<UserCreationProfile>> response = authController.updateProfile(updateRequest, httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Profile updated successfully", response.getBody().message());
        assertEquals(updatedProfile, response.getBody().data());

        verify(authService, times(1)).updateUserProfile(token, updateRequest);
    }

    @Test
    void updateProfile_WithTokenInCookie_ShouldReturnUpdatedProfile() throws Exception {
        // Arrange
        String token = "token-from-cookie";

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "CookieFirstName",
                "CookieLastName"
        );

        UserCreationProfile updatedProfile = UserCreationProfile.builder()
                .id("user456")
                .firstName(updateRequest.firstName())
                .lastName(updateRequest.lastName())
                .build();

        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        Cookie authCookie = new Cookie("Authorization", token);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{authCookie});
        when(authService.updateUserProfile(token, updateRequest)).thenReturn(updatedProfile);

        // Act
        ResponseEntity<BaseResponse<UserCreationProfile>> response = authController.updateProfile(updateRequest, httpServletRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Profile updated successfully", response.getBody().message());
        assertEquals(updatedProfile, response.getBody().data());

        verify(authService, times(1)).updateUserProfile(token, updateRequest);
    }

    @Test
    void updateProfile_MissingToken_ShouldThrowAccessDeniedException() throws JsonProcessingException {
        // Arrange
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getCookies()).thenReturn(null);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "MissingFirstName",
                "MissingLastName"
        );

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authController.updateProfile(updateRequest, httpServletRequest));

        assertNotNull(ex);
        verify(authService, never()).updateUserProfile(anyString(), any());
    }
    @Test
    void extractAccessToken_FromHeader_Success() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer my-access-token");

        String result = authController.extractAccessToken(request);
        Assertions.assertEquals("my-access-token", result);
    }

    @Test
    void extractAccessToken_FromCookie_Success() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn(null);

        Cookie[] cookies = {
                new Cookie("Authorization", " my-cookie-token ")
        };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        String result = controller.extractAccessToken(request);
        Assertions.assertEquals("my-cookie-token", result);
    }

    @Test
    void extractAccessToken_MissingToken_ThrowsException() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn(null);
        Mockito.when(request.getCookies()).thenReturn(null);

        AccessDeniedException exception = Assertions.assertThrows(
                AccessDeniedException.class,
                () -> controller.extractAccessToken(request)
        );

        Assertions.assertEquals(
                Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION,
                exception.getMessage()
        );
    }

    @Test
    void extractAccessToken_HeaderInvalid_ThenGetFromCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        Cookie[] cookies = {
                new Cookie("Authorization", "  abc-xyz-cookie-token ")
        };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        String result = controller.extractAccessToken(request);
        Assertions.assertEquals("abc-xyz-cookie-token", result);
    }

    @Test
    void extractAccessToken_HeaderEmptyToken_ThenGetFromCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer    "); // token rỗng

        Cookie[] cookies = {
                new Cookie("Authorization", "cookie-real-token")
        };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        String result = controller.extractAccessToken(request);
        Assertions.assertEquals("cookie-real-token", result);
    }


}
