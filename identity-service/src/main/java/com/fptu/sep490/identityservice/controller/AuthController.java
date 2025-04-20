package com.fptu.sep490.identityservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.fptu.sep490.identityservice.viewmodel.UserAccessInfo;
import com.fptu.sep490.identityservice.viewmodel.UserCreationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthController {
    AuthService authService;

    @PostMapping("/sign-in")
    @Operation(
            summary = "Login with username and password",
            description = "Authenticate user and return Keycloak access token in cookies and body.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            ), responses = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = KeyCloakTokenResponse.class)),
                    headers = {@Header(
                            name = "Set-Cookie",
                            description = "Set cookies for access and refresh tokens",
                            required = true,
                            schema = @Schema(type = "string"))}),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    }
    )
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> signIn(@RequestBody LoginRequest loginRequest,
                                                                      HttpServletResponse response)
            throws JsonProcessingException {
        KeyCloakTokenResponse loginResponse = authService.login(loginRequest.email(), loginRequest.password());
        CookieUtils.setTokenCookies(response, loginResponse);
        return ResponseEntity.ok(BaseResponse.<KeyCloakTokenResponse>builder()
                .data(loginResponse)
                .build());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Refresh the access token using a valid refresh token stored in cookies.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token refreshed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = KeyCloakTokenResponse.class)
                            ),
                            headers = {
                                    @Header(name = "Set-Cookie", description = "New access/refresh token cookies")
                            }
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - refresh token is missing or invalid"
                    )
            }
    )
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> refreshToken(HttpServletRequest request,
                                                                            HttpServletResponse response) {
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new UnauthorizedException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED);
        }
        KeyCloakTokenResponse refreshedToken = authService.refreshToken(refreshToken);
        CookieUtils.setTokenCookies(response, refreshedToken);
        return ResponseEntity.ok(BaseResponse.<KeyCloakTokenResponse>builder()
                .data(refreshedToken)
                .build());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Logout the currently authenticated user by clearing the access and refresh tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or missing refresh token")
    })
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SignInRequiredException(Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION,
                    Constants.ErrorCode.SIGN_IN_REQUIRE_EXCEPTION);
        }
        authService.logout(accessToken, refreshToken);
        CookieUtils.clearCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/introspect")
    @Operation(
            summary = "Token introspection",
            description = "Check and return information about the current access token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid, introspection result returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    public ResponseEntity<BaseResponse<IntrospectResponse>> introspect(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        IntrospectResponse introspectResponse = authService.introspect(accessToken);
        return ResponseEntity.ok(BaseResponse.<IntrospectResponse>builder()
                .data(introspectResponse)
                .build());

    }

    @PostMapping("/sign-up")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user in Keycloak and returns the location of the profile resource."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully, returns Location header"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username already exists", content = @Content)
    })
    public ResponseEntity<?> register(@RequestBody UserCreationRequest userCreationRequest)
            throws JsonProcessingException {
        String userId = authService.createUser(userCreationRequest);
        URI location = URI.create("/api/v1/profile/" + userId);
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/send/verify/{email}")
    public ResponseEntity<?> sendVerifyEmail(@PathVariable("email") String email) throws JsonProcessingException {
        authService.sendVerifyEmail(email);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/verify-email/status")
    @Operation(
            summary = "Check email verification status",
            description = "Check if the email is verified or not"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<UserAccessInfo>> checkEmailVerificationStatus(HttpServletRequest request)
            throws JsonProcessingException {
        String accessToken = extractAccessToken(request);
        UserAccessInfo userAccessInfo = authService.getUserAccessInfo(accessToken);
        return ResponseEntity.ok(BaseResponse.<UserAccessInfo>builder()
                .data(userAccessInfo)
                .build());
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank() || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException(Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION, com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage.ACCESS_DENIED);
        }
        String token = header.substring(7);
        if (token.isBlank()) {
            throw new AccessDeniedException(Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION, com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage.ACCESS_DENIED);
        }
        return token;
    }
}
