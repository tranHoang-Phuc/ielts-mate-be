package com.fptu.sep490.identityservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.CookieConstants;
import com.fptu.sep490.commonlibrary.exceptions.AccessDeniedException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.viewmodel.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/auth")
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
    })
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> signIn(@RequestBody @Valid LoginRequest loginRequest,
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
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or missing refresh token")
    })
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED);
        }
        authService.logout(accessToken, refreshToken);
        CookieUtils.clearCookie(response);
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("Logout successful")
                .build());
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
    public ResponseEntity<BaseResponse<UserCreationProfile>> register(@RequestBody @Valid UserCreationRequest userCreationRequest)
            throws Exception {
        UserCreationProfile userProfile = authService.createUser(userCreationRequest);
        return new ResponseEntity<>(BaseResponse.<UserCreationProfile>builder()
                .data(userProfile)
                .build(), HttpStatus.CREATED);
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

    @PostMapping("/verify-email/send-otp")
    @Operation(
            summary = "Send OTP for email verification",
            description = "Send an OTP to the user's email address for verification"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    })
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest sendOtpRequest)
            throws JsonProcessingException {
        authService.sendVerifyEmail(sendOtpRequest.email());
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("OTP sent successfully")
                .build());
    }

    @PostMapping("/verify-email/verify")
    @Operation(
            summary = "Verify email",
            description = "Verify the user's email address"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Email verified successfully"),
            @ApiResponse(responseCode = "409", description = "Email not valid", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    })
    public ResponseEntity<BaseResponse<KeyCloakTokenResponse>> verifyEmail(@RequestBody VerifyEmailRequest verifyEmailRequest,
                                                                           HttpServletResponse response)
            throws Exception {
        var data = authService.verifyEmail(verifyEmailRequest.email(), verifyEmailRequest.otp());
        CookieUtils.setTokenCookies(response, data);
        return ResponseEntity.ok(BaseResponse.<KeyCloakTokenResponse>builder()
                .message("Email verified successfully")
                .data(data)
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Reset the password for the user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest)
            throws JsonProcessingException {
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("Password reset successfully")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Forgot password",
            description = "Send a reset password email to the user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Forgot password email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    })
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest)
            throws JsonProcessingException {
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("Forgot password email sent successfully")
                .build());
    }

    @PostMapping("/verify-reset-token")
    @Operation(
            summary = "Verify reset token",
            description = "Verify the reset password token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reset token verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    })
    public ResponseEntity<?> verifyResetToken(@RequestBody VerifyResetTokenRequest verifyResetTokenRequest) {
        authService.checkResetPasswordToken(verifyResetTokenRequest.email(), verifyResetTokenRequest.otp());
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("Reset token verified successfully")
                .build());
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get user profile",
            description = "Get the profile of the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserAccessInfo.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<UserCreationProfile>> getProfile(HttpServletRequest request) throws JsonProcessingException {
        String accessToken = extractAccessToken(request);
        UserCreationProfile userAccessInfo = authService.getUserProfile(accessToken);
        return ResponseEntity.ok(BaseResponse.<UserCreationProfile>builder()
                .data(userAccessInfo)
                .build());
    }

    @GetMapping("/google")
    @Operation(
            summary = "Login with Google",
            description = "Get the Google login URL and redirect the user to it"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Google login URL"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    })
    public ResponseEntity<Void> loginWithGoogle() throws URISyntaxException {
        String authUrl = authService.createGoogleUrl();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(new URI(authUrl))
                .build();
    }

    @PutMapping("/change-password")
    @Operation(
            summary = "Change user password",
            description = "Change the password of the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> changePassword(@RequestBody @Valid PasswordChange changePasswordRequest,
                                                              HttpServletRequest request) throws JsonProcessingException {
        String accessToken = extractAccessToken(request);
        authService.changePassword(accessToken, changePasswordRequest);
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .data(null)
                .message("Password changed successfully")
                .build());
    }

    @PutMapping("/update-profile")
    @Operation(
            summary = "Update user profile",
            description = "Update the profile of the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<UserCreationProfile>> updateProfile(@RequestBody @Valid UserUpdateRequest userCreationRequest,
                                                                 HttpServletRequest request) throws Exception {
        String accessToken = extractAccessToken(request);
        UserCreationProfile updatedProfile = authService.updateUserProfile(accessToken, userCreationRequest);
        return ResponseEntity.ok(BaseResponse.<UserCreationProfile>builder()
                .data(updatedProfile)
                .message("Profile updated successfully")
                .build());
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && !header.isBlank() && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isBlank()) {
                return token;
            }
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Authorization".equalsIgnoreCase(cookie.getName())) {
                    String token = cookie.getValue() != null ? cookie.getValue().trim() : "";
                    if (!token.isBlank()) {
                        return token;
                    }
                }
            }
        }
        throw new AccessDeniedException(
                Constants.ErrorCodeMessage.SIGN_IN_REQUIRE_EXCEPTION,
                com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage.ACCESS_DENIED
        );
    }



}
