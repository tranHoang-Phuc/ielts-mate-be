package com.fptu.sep490.identityservice.controller;

import com.fptu.sep490.commonlibrary.constants.CookieConstants;
import com.fptu.sep490.commonlibrary.exceptions.AccessDeniedException;
import com.fptu.sep490.commonlibrary.exceptions.SignInRequiredException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.LoginRequest;
import com.fptu.sep490.identityservice.viewmodel.UserCreationParam;
import com.fptu.sep490.identityservice.viewmodel.UserCreationRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthController {
    AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password",
            description = "Authenticate a user and return Keycloak access token in cookies and body.")
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> login(@RequestBody LoginRequest loginRequest,
                                                                     HttpServletResponse response) {
        KeyCloakTokenResponse loginResponse = authService.login(loginRequest.username(), loginRequest.password());
        CookieUtils.setTokenCookies(response, loginResponse);
        return ResponseEntity.ok(BaseResponse.<KeyCloakTokenResponse>builder()
                .data(loginResponse)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> refreshToken(HttpServletRequest request,
                                                                            HttpServletResponse response) {
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new UnauthorizedException(Constants.ErrorCode.UNAUTHORIZED);
        }
        KeyCloakTokenResponse refreshedToken = authService.refreshToken(refreshToken);
        CookieUtils.setTokenCookies(response, refreshedToken);
        return ResponseEntity.ok(BaseResponse.<KeyCloakTokenResponse>builder()
                .data(refreshedToken)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SignInRequiredException(Constants.ErrorCode.SIGN_IN_REQUIRE_EXCEPTION);
        }
        authService.logout(accessToken, refreshToken);
        CookieUtils.clearCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/introspect")
    public ResponseEntity<BaseResponse<IntrospectResponse>> introspect(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        IntrospectResponse introspectResponse = authService.introspect(accessToken);
            return ResponseEntity.ok(BaseResponse.<IntrospectResponse>builder()
                    .data(introspectResponse)
                    .build());

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserCreationRequest userCreationRequest) {
        return null;
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank() || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException(Constants.ErrorCode.SIGN_IN_REQUIRE_EXCEPTION);
        }
        String token = header.substring(7);
        if (token.isBlank()) {
            throw new AccessDeniedException(Constants.ErrorCode.SIGN_IN_REQUIRE_EXCEPTION);
        }
        return token;
    }
}
