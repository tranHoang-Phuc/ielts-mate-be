package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.*;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.identityservice.component.AesSecretKey;
import com.fptu.sep490.event.EmailSendingRequest;
import com.fptu.sep490.event.RecipientUser;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.exception.ErrorNormalizer;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.service.AuthService;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.identityservice.service.EmailTemplateService;
import com.fptu.sep490.identityservice.service.ForgotPasswordRateLimiter;
import com.fptu.sep490.identityservice.service.VerifyEmailRateLimiter;
import com.fptu.sep490.identityservice.viewmodel.*;
import com.fptu.sep490.event.VerificationRequest;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    ErrorNormalizer errorNormalizer;
    RedisService redisService;
    KafkaTemplate<String, Object> kafkaTemplate;
    EmailTemplateService emailTemplateService;
    ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    VerifyEmailRateLimiter verifyEmailRateLimiter;
    AesSecretKey aesSecretKey;

    @Value("${keycloak.base-uri}")
    @NonFinal
    String kcUrl;

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

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    @NonFinal
    String issuerUri;

    @Value("${client.domain}")
    @NonFinal
    String clientDomain;

    @Value("${keycloak.redirect-uri}")
    @NonFinal
    String redirectUri;
    @Override
    public KeyCloakTokenResponse login(String username, String password) throws JsonProcessingException {
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos =
                keyCloakUserClient.getUserByEmail(realm, "Bearer " + clientToken, username);
        if (userAccessInfos.isEmpty() || userAccessInfos.getFirst() == null) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND,
                    Constants.ErrorCode.USER_NOT_FOUND);
        }
        if(!userAccessInfos.getFirst().emailVerified()) {
            throw new AppException(Constants.ErrorCodeMessage.EMAIL_NOT_VERIFIED,
                    Constants.ErrorCode.EMAIL_NOT_VERIFIED, HttpStatus.UNAUTHORIZED.value());
        }
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
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        return keyCloakTokenClient.requestToken(form, realm);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
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
    public UserCreationProfile createUser(UserCreationRequest request) throws Exception {
        String clientToken = getCachedClientToken();
        UserCreationParam userCreationParam = UserCreationParam.builder()
                .username(request.email())
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
            String id = extractUserId(creationResponse);
            String encryptedPassword = aesSecretKey.encrypt(request.password());
            redisService.saveValue(getPasswordKey(request.email()), encryptedPassword);
            UserCreationProfile userCreationProfile = UserCreationProfile.builder()
                    .id(id)
                    .email(request.email())
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .build();
            sendVerifyEmail(request.email());
            return userCreationProfile;
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }

    }

    @Override
    public void sendVerifyEmail(String email) throws JsonProcessingException {
        if (verifyEmailRateLimiter.isBlocked(email)) {
            throw new TooManyRequestException(Constants.ErrorCodeMessage.VERIFY_EMAIL_RATE_LIMIT,
                    Constants.ErrorCode.VERIFY_EMAIL_RATE_LIMIT);
        }
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer " + clientToken, email);
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND, Constants.ErrorCode.USER_NOT_FOUND);
        }
        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        if(userAccessInfo.emailVerified()) {
            throw new ConflictException(Constants.ErrorCodeMessage.EMAIL_ALREADY_VERIFIED,
                    Constants.ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        String userId = userAccessInfo.id();
        String otp = generateAndStoreOtp(email);
        verifyEmailRateLimiter.recordAttempt(email);
        VerificationRequest verificationRequest = VerificationRequest.builder()
                .token(otp)
                .build();
        String htmlContent = emailTemplateService.buildVerificationEmail(otp);
        RecipientUser recipientUser = RecipientUser.builder()
                .email(email)
                .firstName(userAccessInfo.firstName())
                .lastName(userAccessInfo.lastName())
                .userId(userId)
                .build();
        EmailSendingRequest<VerificationRequest> emailSendingRequest = EmailSendingRequest.<VerificationRequest>builder()
                .recipientUser(recipientUser)
                .htmlContent(htmlContent).subject("Verify email")
                .data(verificationRequest)
                .build();
        kafkaTemplate.send(userVerificationTopic, emailSendingRequest);
    }

    @Override
    public UserAccessInfo getUserAccessInfo(String accessToken) throws JsonProcessingException {
        String username = getUsernameFromToken(accessToken);
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer " + clientToken, username);
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND, username);
        }
        return userAccessInfos.getFirst();
    }

    @Override
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) throws JsonProcessingException {
        if (!isValidToken(resetPasswordRequest.token(), "reset-password", "reset-password")) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
        if (!resetPasswordRequest.password().equals(resetPasswordRequest.confirmPassword())) {
            throw new ConflictException(Constants.ErrorCodeMessage.CONFLICT_PASSWORD,
                    Constants.ErrorCode.CONFLICT_PASSWORD);
        }
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer  " +
                getCachedClientToken(), resetPasswordRequest.email());
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND,
                    Constants.ErrorCode.USER_NOT_FOUND, resetPasswordRequest.email());
        }

        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        String userId = userAccessInfo.id();
        try {
            keyCloakUserClient.resetPassword(realm, "Bearer " + getCachedClientToken(), userId,
                    ChangePasswordRequest.builder()
                            .type("password")
                            .temporary(false)
                            .value(resetPasswordRequest.password())
                            .build());
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }

    }


    @Override
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws JsonProcessingException {
        if (forgotPasswordRateLimiter.isBlocked(forgotPasswordRequest.email())) {
            throw new TooManyRequestException(Constants.ErrorCodeMessage.FORGOT_PASSWORD_RATE_LIMIT,
                    Constants.ErrorCode.FORGOT_PASSWORD_RATE_LIMIT);
        }
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer " + clientToken,
                forgotPasswordRequest.email());
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND, forgotPasswordRequest.email());
        }
        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        String userId = userAccessInfo.id();
        String verifyToken = generateEmailVerifyToken(userId, forgotPasswordRequest.email(),
                "reset-password", "reset-password");
        forgotPasswordRateLimiter.recordAttempt(forgotPasswordRequest.email());
        VerificationRequest verificationRequest = VerificationRequest.builder()
                .token(verifyToken)
                .build();
        String tokenParam = URLEncoder.encode(verifyToken, StandardCharsets.UTF_8);
        String emailParam = URLEncoder.encode(forgotPasswordRequest.email(), StandardCharsets.UTF_8);
        String url = clientDomain + "/reset?token=" + tokenParam + "&email=" + emailParam;
        String htmlContent = emailTemplateService.buildForgotPasswordEmail(url);
        RecipientUser recipientUser = RecipientUser.builder()
                .email(forgotPasswordRequest.email())
                .firstName(userAccessInfo.firstName())
                .lastName(userAccessInfo.lastName())
                .userId(userId)
                .build();
        EmailSendingRequest<VerificationRequest> emailSendingRequest = EmailSendingRequest.<VerificationRequest>builder()
                .recipientUser(recipientUser)
                .htmlContent(htmlContent).subject("Forgot password")
                .data(verificationRequest)
                .build();
        kafkaTemplate.send(userVerificationTopic, emailSendingRequest);
    }

    @Override
    public KeyCloakTokenResponse verifyEmail(String email, String otp) throws Exception {
        String otpInCache = redisService.getValue("otp:" + email, String.class);
        if (otpInCache == null) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
        if (!otpInCache.equals(otp)) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm,
                "Bearer " + clientToken, email);
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND,
                    Constants.ErrorCode.USER_NOT_FOUND, email);
        }
        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        String userId = userAccessInfo.id();
        try {
            keyCloakUserClient.verifyEmail(realm, "Bearer " + clientToken, userId,
                    VerifyParam.builder()
                            .emailVerified(true)
                            .build());
            String htmlContent = emailTemplateService.buildEmailVerificationSuccess(userAccessInfo.email(),
                    userAccessInfo.firstName() + userAccessInfo.lastName());
            RecipientUser recipientUser = RecipientUser.builder()
                    .email(userAccessInfo.email())
                    .firstName(userAccessInfo.firstName())
                    .lastName(userAccessInfo.lastName())
                    .userId(userId)
                    .build();
            EmailSendingRequest<VerificationRequest> emailSendingRequest = EmailSendingRequest.<VerificationRequest>builder()
                    .recipientUser(recipientUser)
                    .htmlContent(htmlContent).subject("Verify email successfully")
                    .data(VerificationRequest.builder().build())
                    .build();
            kafkaTemplate.send(userVerificationTopic, emailSendingRequest);
            redisService.delete("otp:" + email);
            String encryptedPassword = redisService.getValue(getPasswordKey(email), String.class);
            String normalPassword = aesSecretKey.decrypt(encryptedPassword);
            redisService.delete(getPasswordKey(email));
            return login(email, normalPassword);

        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    @Override
    public void verifyResetToken(String email, String otp) {

        isValidToken(otp, "reset-password", "reset-password");
    }
    @Override
    public void checkResetPasswordToken(String email, String otp) {
        isValidCheckedToken(otp, "reset-password", "reset-password");
    }

    @Override
    public void changePassword(String accessToken, PasswordChange changePasswordRequest) throws JsonProcessingException {
        String email = getEmailFromToken(accessToken);
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("username", email);
            form.add("password", changePasswordRequest.oldPassword());
            form.add("scope", "openid");
            keyCloakTokenClient.requestToken(form, realm);
        } catch (FeignException exception) {
            throw new AppException(Constants.ErrorCodeMessage.WRONG_OLD_PASSWORD,
                    Constants.ErrorCode.WRONG_OLD_PASSWORD, HttpStatus.BAD_REQUEST.value());
        }
        if (!changePasswordRequest.newPassword().equals(changePasswordRequest.confirmNewPassword())) {
            throw new ConflictException(Constants.ErrorCodeMessage.CONFLICT_PASSWORD,
                    Constants.ErrorCode.CONFLICT_PASSWORD);
        }

        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer  " +
                getCachedClientToken(), email);
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND,
                    Constants.ErrorCode.USER_NOT_FOUND, email);
        }

        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        String userId = userAccessInfo.id();

        try {
            keyCloakUserClient.resetPassword(realm, "Bearer " + clientToken, userId,
                    ChangePasswordRequest.builder()
                            .type("password")
                            .temporary(false)
                            .value(changePasswordRequest.newPassword())
                            .build());
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }

    }

    @Override
    public UserCreationProfile updateUserProfile(String accessToken, UserUpdateRequest userUpdateRequest) throws JsonProcessingException {
        String email = getEmailFromToken(accessToken);
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient
                .getUserByEmail(realm, "Bearer " + clientToken, email);
        if (userAccessInfos.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.USER_NOT_FOUND, Constants.ErrorCode.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value());
        }
        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        String userId = userAccessInfo.id();
        Map<String, Object> updates = Map.of(
                "firstName", userUpdateRequest.firstName(),
                "lastName", userUpdateRequest.lastName()
        );
        try {
            ResponseEntity<?> response = keyCloakUserClient.updateUserProfile(realm, "Bearer " + clientToken,
                    userId, updates);
            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new InternalServerErrorException(Constants.ErrorCodeMessage.KEYCLOAK_ERROR,
                        Constants.ErrorCode.KEYCLOAK_ERROR);
            }
            return UserCreationProfile.builder()
                    .id(userAccessInfo.id())
                    .email(userAccessInfo.email())
                    .firstName(userUpdateRequest.firstName())
                    .lastName(userUpdateRequest.lastName())
                    .build();
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    @Override
    public String createGoogleUrl() {
        return String.format(
                "%s/realms/%s/protocol/openid-connect/auth" +
                        "?client_id=%s" +
                        "&redirect_uri=%s" +
                        "&response_type=code" +
                        "&scope=openid" +
                        "&kc_idp_hint=google",
                kcUrl, realm, clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        );
    }

    @Override
    public UserCreationProfile getUserProfile(String accessToken) throws JsonProcessingException {
        String username = getUsernameFromToken(accessToken);
        String clientToken = getCachedClientToken();
        List<UserAccessInfo> userAccessInfos = keyCloakUserClient.getUserByEmail(realm, "Bearer " + clientToken, username);
        if (userAccessInfos.isEmpty()) {
            throw new NotFoundException(Constants.ErrorCodeMessage.USER_NOT_FOUND, username);
        }
        UserAccessInfo userAccessInfo = userAccessInfos.getFirst();
        return UserCreationProfile.builder()
                .id(userAccessInfo.id())
                .email(userAccessInfo.email())
                .firstName(userAccessInfo.firstName())
                .lastName(userAccessInfo.lastName())
                .build();
    }

    public boolean isValidCheckedToken(String token, String expectedAction, String expectedPurpose) {
        String email = getEmailFromToken(token);
        if (!isTokenValidInCache(email, expectedAction, token)) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(emailVerifySecret.getBytes(StandardCharsets.UTF_8)))
                    .requireIssuer(issuerUri)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                throw new BadRequestException(Constants.ErrorCodeMessage.TIME_OUT_TOKEN,
                        Constants.ErrorCode.TIME_OUT_TOKEN);
            }

            String action = claims.get("action", String.class);
            String purpose = claims.get("purpose", String.class);

            if (!expectedAction.equals(action) || !expectedPurpose.equals(purpose)) {
                throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                        Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
            }
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
    }
    public boolean isValidToken(String token, String expectedAction, String expectedPurpose) {
        String email = getEmailFromToken(token);
        if (!isTokenValidInCache(email, expectedAction, token)) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(emailVerifySecret.getBytes(StandardCharsets.UTF_8)))
                    .requireIssuer(issuerUri)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                throw new BadRequestException(Constants.ErrorCodeMessage.TIME_OUT_TOKEN,
                        Constants.ErrorCode.TIME_OUT_TOKEN);
            }

            String action = claims.get("action", String.class);
            String purpose = claims.get("purpose", String.class);

            if (!expectedAction.equals(action) || !expectedPurpose.equals(purpose)) {
                throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                        Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
            }
            redisService.delete(getVerifyTokenKey(email, action));
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException(Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCode.INVALID_VERIFIED_TOKEN);
        }
    }
    private String generateAndStoreOtp(String email) {
        String otp = generateOTP();

        String redisKey = "otp:" + email;
        try {
            redisService.saveValue(redisKey, otp, Duration.ofMinutes(10));
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(
                    Constants.ErrorCode.REDIS_ERROR,
                    Constants.ErrorCodeMessage.REDIS_ERROR
            );
        }
        return otp;
    }

    public boolean isTokenValidInCache(String email, String action, String token) {
        String redisKey = getVerifyTokenKey(email, action);
        try {
            String cachedToken = redisService.getValue(redisKey, String.class);
            return token.equals(cachedToken);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private String getUsernameFromToken(String accessToken) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        Jwt jwt = decoder.decode(accessToken);
        return jwt.getClaim("preferred_username");
    }

    public String getEmailFromToken(String accessToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(emailVerifySecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            return claims.get("email", String.class);
        } catch (JwtException e) {
            throw new BadRequestException(Constants.ErrorCode.INVALID_VERIFIED_TOKEN,
                    Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN);
        }
    }
    private String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append((int) (Math.random() * 10));
        }
        return otp.toString();
    }
    private String generateEmailVerifyToken(String userId, String email, String action, String purpose) {
        Key key = Keys.hmacShaKeyFor(emailVerifySecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(emailVerifyTokenExpireTime);

        String token = Jwts.builder()
                .setSubject(userId)
                .claim("username", email)
                .claim("email", email)
                .claim("action", action)
                .claim("purpose", purpose)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setIssuer(issuerUri)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        String redisKey = getVerifyTokenKey(email, action);
        try {
            redisService.saveValue(redisKey, token, Duration.ofSeconds(emailVerifyTokenExpireTime));
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(Constants.ErrorCode.REDIS_ERROR,
                    Constants.ErrorCodeMessage.REDIS_ERROR);
        }
        return token;
    }

    private String getPasswordKey(String email) {
        return String.format("password:%s", email);
    }
    private String getVerifyTokenKey(String email, String action) {
        return String.format("verify-token:%s:%s", action, email);
    }

    private String extractUserId(ResponseEntity<?> response) {
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
