package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.*;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.identityservice.component.AesSecretKey;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.exception.ErrorNormalizer;
import com.fptu.sep490.identityservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.identityservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.identityservice.service.EmailTemplateService;
import com.fptu.sep490.identityservice.service.ForgotPasswordRateLimiter;
import com.fptu.sep490.identityservice.service.VerifyEmailRateLimiter;
import com.fptu.sep490.identityservice.viewmodel.*;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
	@InjectMocks
	AuthServiceImpl authService;
	@Mock KeyCloakTokenClient keyCloakTokenClient;
	@Mock KeyCloakUserClient keyCloakUserClient;
	@Mock ErrorNormalizer errorNormalizer;
	@Mock RedisService redisService;
	@Mock KafkaTemplate<String, Object> kafkaTemplate;
	@Mock EmailTemplateService emailTemplateService;
	@Mock ForgotPasswordRateLimiter forgotPasswordRateLimiter;
	@Mock VerifyEmailRateLimiter verifyEmailRateLimiter;
	@Mock AesSecretKey aesSecretKey;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(authService, "kcUrl", "http://kc");
		ReflectionTestUtils.setField(authService, "realm", "realm");
		ReflectionTestUtils.setField(authService, "clientId", "client");
		ReflectionTestUtils.setField(authService, "clientSecret", "secret");
		ReflectionTestUtils.setField(authService, "emailVerifySecret", "12345678901234567890123456789012");
		ReflectionTestUtils.setField(authService, "emailVerifyTokenExpireTime", 3600);
		ReflectionTestUtils.setField(authService, "userVerificationTopic", "topic");
		ReflectionTestUtils.setField(authService, "issuerUri", "issuer");
		ReflectionTestUtils.setField(authService, "clientDomain", "http://client");
		ReflectionTestUtils.setField(authService, "redirectUri", "http://redirect");
	}

	@Test
	void login_success() throws Exception {
		String username = "user";
		String password = "pass";
		KeyCloakTokenResponse tokenResponse = mock(KeyCloakTokenResponse.class);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.emailVerified()).thenReturn(true);
		when(keyCloakUserClient.getUserByEmail(any(), any(), eq(username)))
				.thenReturn(List.of(user));
		when(keyCloakTokenClient.requestToken(any(), any())).thenReturn(tokenResponse);

		KeyCloakTokenResponse result = authService.login(username, password);
		assertEquals(tokenResponse, result);
	}

	@Test
	void login_userNotFound() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(List.of());
		assertThrows(NotFoundException.class, () -> authService.login("user", "pass"));
	}

	@Test
	void login_firstElementNull() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		java.util.List<UserAccessInfo> listWithNull = java.util.Arrays.asList((UserAccessInfo) null);
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(listWithNull);
		assertThrows(NotFoundException.class, () -> authService.login("user", "pass"));
	}

	@Test
	void login_emailNotVerified() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.emailVerified()).thenReturn(false);
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(List.of(user));
		assertThrows(AppException.class, () -> authService.login("accounttestnotverify@gmail.com", "123456aA@"));
	}

	@Test
	void login_feignException() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.emailVerified()).thenReturn(true);
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(List.of(user));
		FeignException fe = mock(FeignException.class);
		when(keyCloakTokenClient.requestToken(any(), any())).thenThrow(fe);
		when(errorNormalizer.handleKeyCloakException(fe)).thenThrow(new AppException("err", "err", 400));
		assertThrows(AppException.class, () -> authService.login("user", "pass"));
	}

	@Test
	void refreshToken_success() {
		KeyCloakTokenResponse tokenResponse = mock(KeyCloakTokenResponse.class);
		when(keyCloakTokenClient.requestToken(any(), any())).thenReturn(tokenResponse);
		assertEquals(tokenResponse, authService.refreshToken("refresh"));
	}

	@Test
	void introspect_buildsFormAndCallsClient() {
		IntrospectResponse expected = mock(IntrospectResponse.class);
		ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
		when(keyCloakTokenClient.introspect(eq("realm"), formCaptor.capture())).thenReturn(expected);

		IntrospectResponse actual = authService.introspect("accessToken");

		assertEquals(expected, actual);
		MultiValueMap<String, String> form = formCaptor.getValue();
		assertEquals("client", form.getFirst("client_id"));
		assertEquals("secret", form.getFirst("client_secret"));
		assertEquals("accessToken", form.getFirst("token"));
	}

	@Test
	void logout_callsKeycloakWithProperForm() {
		ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
		doNothing().when(keyCloakTokenClient).logout(eq("realm"), formCaptor.capture(), eq("Bearer accessToken"));
		authService.logout("accessToken", "refreshToken");
		MultiValueMap<String, String> form = formCaptor.getValue();
		assertEquals("client", form.getFirst("client_id"));
		assertEquals("secret", form.getFirst("client_secret"));
		assertEquals("refreshToken", form.getFirst("refresh_token"));
	}

	@Test
	void createUser_success() throws Exception {
		UserCreationRequest req = mock(UserCreationRequest.class);
		when(req.email()).thenReturn("e@x.com");
		when(req.firstName()).thenReturn("First");
		when(req.lastName()).thenReturn("Last");
		when(req.password()).thenReturn("pass");
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.add("Location", "http://kc/admin/realms/realm/users/abc123");
		org.springframework.http.ResponseEntity<Void> kcResponse = new org.springframework.http.ResponseEntity<>(null, headers, org.springframework.http.HttpStatus.CREATED);
		doReturn(kcResponse)
				.when(keyCloakUserClient)
				.createUser(anyString(), anyString(), any(UserCreationParam.class));
		when(aesSecretKey.encrypt("pass")).thenReturn("enc");
		doNothing().when(redisService).saveValue(eq("password:e@x.com"), eq("enc"));

		UserCreationProfile profile = authService.createUser(req);
		assertEquals("abc123", profile.id());
		assertEquals("e@x.com", profile.email());
		assertEquals("First", profile.firstName());
		assertEquals("Last", profile.lastName());
	}

	@Test
	void createUser_feignException() throws Exception {
		UserCreationRequest req = mock(UserCreationRequest.class);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		FeignException fe = mock(FeignException.class);
		when(keyCloakUserClient.createUser(anyString(), anyString(), any(UserCreationParam.class))).thenThrow(fe);
		when(errorNormalizer.handleKeyCloakException(fe)).thenThrow(new AppException("err", "err", 400));
		assertThrows(AppException.class, () -> authService.createUser(req));
	}

	@Test
	void sendVerifyEmail_success() throws Exception {
		String email = "e";
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.emailVerified()).thenReturn(false);
		when(user.id()).thenReturn("id");
		when(user.firstName()).thenReturn("f");
		when(user.lastName()).thenReturn("l");
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(any(), any(), eq(email))).thenReturn(List.of(user));
		when(emailTemplateService.buildVerificationEmail(any())).thenReturn("html");
		doNothing().when(redisService).saveValue(any(), any(), any());
		doNothing().when(verifyEmailRateLimiter).recordAttempt(any());
		when(verifyEmailRateLimiter.isBlocked(email)).thenReturn(false);
		authService.sendVerifyEmail(email);
		verify(kafkaTemplate).send(any(), any());
	}

	@Test
	void sendVerifyEmail_rateLimited() throws JsonProcessingException {
		when(verifyEmailRateLimiter.isBlocked(any())).thenReturn(true);
		assertThrows(TooManyRequestException.class, () -> authService.sendVerifyEmail("e"));
	}

	@Test
	void sendVerifyEmail_userNotFound() throws JsonProcessingException {
		when(verifyEmailRateLimiter.isBlocked(any())).thenReturn(false);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(List.of());
		assertThrows(NotFoundException.class, () -> authService.sendVerifyEmail("phuc1234@gmail.com"));
	}

	@Test
	void sendVerifyEmail_alreadyVerified() throws JsonProcessingException {
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.emailVerified()).thenReturn(true);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(any(), any(), any())).thenReturn(List.of(user));
		when(verifyEmailRateLimiter.isBlocked(any())).thenReturn(false);
		assertThrows(ConflictException.class, () -> authService.sendVerifyEmail("e"));
	}

	@Test
	void getUserAccessInfo_success() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo info = mock(UserAccessInfo.class);
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("user@example.com")))
					.thenReturn(List.of(info));

			UserAccessInfo result = authService.getUserAccessInfo("accessToken");
			assertEquals(info, result);
		}
	}

	@Test
	void getUserAccessInfo_userNotFound() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("user@example.com")))
					.thenReturn(List.of());

			assertThrows(NotFoundException.class, () -> authService.getUserAccessInfo("accessToken"));
		}
	}

	@Test
	void getUserAccessInfoByEmail_success() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		String email = "target@example.com";
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo info = mock(UserAccessInfo.class);
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq(email))).thenReturn(List.of(info));
			UserAccessInfo result = authService.getUserAccessInfoByEmail(email, "accessToken");
			assertEquals(info, result);
		}
	}

	@Test
	void getUserAccessInfoByEmail_userNotFound() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		String email = "missing@example.com";
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq(email))).thenReturn(List.of());
			assertThrows(NotFoundException.class, () -> authService.getUserAccessInfoByEmail(email, "accessToken"));
		}
	}

	@Test
	void resetPassword_invalidToken_throwsBadRequest() throws Exception {
		AuthServiceImpl spyService = spy(authService);
		ResetPasswordRequest req = new ResetPasswordRequest("tok", "e@x.com", "a", "a");
		doReturn(false).when(spyService).isValidToken(anyString(), anyString(), anyString());
		assertThrows(BadRequestException.class, () -> spyService.resetPassword(req));
	}

	@Test
	void resetPassword_passwordMismatch_throwsConflict() throws Exception {
		AuthServiceImpl spyService = spy(authService);
		ResetPasswordRequest req = new ResetPasswordRequest("tok", "e@x.com", "a", "b");
		doReturn(true).when(spyService).isValidToken(anyString(), anyString(), anyString());
		assertThrows(ConflictException.class, () -> spyService.resetPassword(req));
	}

	@Test
	void resetPassword_userNotFound_throwsNotFound() throws Exception {
		AuthServiceImpl spyService = spy(authService);
		// order: confirmPassword, email, password, token
		ResetPasswordRequest req = new ResetPasswordRequest("a", "e@x.com", "a", "tok");
		doReturn(true).when(spyService).isValidToken(anyString(), anyString(), anyString());
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of());
		assertThrows(NotFoundException.class, () -> spyService.resetPassword(req));
	}

	@Test
	void resetPassword_feignException_normalized() throws Exception {
		AuthServiceImpl spyService = spy(authService);
		ResetPasswordRequest req = new ResetPasswordRequest("a", "e@x.com", "a", "tok");
		doReturn(true).when(spyService).isValidToken(anyString(), anyString(), anyString());
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.id()).thenReturn("uid");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
		FeignException fe = mock(FeignException.class);
		doThrow(fe).when(keyCloakUserClient).resetPassword(eq("realm"), anyString(), eq("uid"), any(ChangePasswordRequest.class));
		when(errorNormalizer.handleKeyCloakException(fe)).thenThrow(new AppException("err", "err", 400));
		assertThrows(AppException.class, () -> spyService.resetPassword(req));
	}

	@Test
	void resetPassword_success() throws Exception {
		AuthServiceImpl spyService = spy(authService);
		ResetPasswordRequest req = new ResetPasswordRequest("a", "e@x.com", "a", "tok");
		doReturn(true).when(spyService).isValidToken(anyString(), anyString(), anyString());
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.id()).thenReturn("uid");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
		doReturn(ResponseEntity.noContent().build()).when(keyCloakUserClient)
				.resetPassword(eq("realm"), anyString(), eq("uid"), any(ChangePasswordRequest.class));
		assertDoesNotThrow(() -> spyService.resetPassword(req));
	}

	@Test
	void forgotPassword_rateLimited_throwsTooManyRequest() throws Exception {
		String email = "e@e.com";
		when(forgotPasswordRateLimiter.isBlocked(email)).thenReturn(true);
		assertThrows(TooManyRequestException.class, () -> authService.forgotPassword(new ForgotPasswordRequest(email)));
	}

	@Test
	void forgotPassword_userNotFound_throwsNotFound() throws Exception {
		String email = "e@e.com";
		when(forgotPasswordRateLimiter.isBlocked(email)).thenReturn(false);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq(email))).thenReturn(List.of());
		assertThrows(NotFoundException.class, () -> authService.forgotPassword(new ForgotPasswordRequest(email)));
	}

	@Test
	void forgotPassword_success_sendsEmailAndRecordsAttempt() throws Exception {
		String email = "e@e.com";
		when(forgotPasswordRateLimiter.isBlocked(email)).thenReturn(false);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.id()).thenReturn("uid");
		when(user.firstName()).thenReturn("First");
		when(user.lastName()).thenReturn("Last");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq(email))).thenReturn(List.of(user));
		when(emailTemplateService.buildForgotPasswordEmail(anyString())).thenReturn("html");
		doNothing().when(redisService).saveValue(startsWith("verify-token:"), anyString(), any());
		doNothing().when(forgotPasswordRateLimiter).recordAttempt(email);
		assertDoesNotThrow(() -> authService.forgotPassword(new ForgotPasswordRequest(email)));
		verify(forgotPasswordRateLimiter).recordAttempt(email);
		verify(kafkaTemplate).send(eq("topic"), any());
	}

	@Test
	void verifyEmail_otpNull_throwsBadRequest() throws Exception {
		when(redisService.getValue("otp:e@example.com", String.class)).thenReturn(null);
		assertThrows(BadRequestException.class, () -> authService.verifyEmail("e@example.com", "123456"));
	}

	@Test
	void verifyEmail_otpMismatch_throwsBadRequest() throws Exception {
		when(redisService.getValue("otp:e@example.com", String.class)).thenReturn("654321");
		assertThrows(BadRequestException.class, () -> authService.verifyEmail("e@example.com", "123456"));
	}

	@Test
	void verifyEmail_userNotFound_throwsNotFound() throws Exception {
		when(redisService.getValue("otp:e@example.com", String.class)).thenReturn("123456");
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@example.com"))).thenReturn(List.of());
		assertThrows(NotFoundException.class, () -> authService.verifyEmail("e@example.com", "123456"));
	}

	@Test
	void verifyEmail_feignException_normalized() throws Exception {
		when(redisService.getValue("otp:e@example.com", String.class)).thenReturn("123456");
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.id()).thenReturn("uid");
		when(user.email()).thenReturn("e@example.com");
		when(user.firstName()).thenReturn("First");
		when(user.lastName()).thenReturn("Last");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@example.com"))).thenReturn(List.of(user));
		FeignException fe = mock(FeignException.class);
		doThrow(fe).when(keyCloakUserClient).verifyEmail(eq("realm"), anyString(), eq("uid"), any(VerifyParam.class));
		when(errorNormalizer.handleKeyCloakException(fe)).thenThrow(new AppException("err", "err", 400));
		assertThrows(AppException.class, () -> authService.verifyEmail("e@example.com", "123456"));
	}

	@Test
	void verifyEmail_success_returnsTokenResponse() throws Exception {
		// OTP checks
		when(redisService.getValue("otp:e@example.com", String.class)).thenReturn("123456");
		// Client token and user lookup
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserAccessInfo user = mock(UserAccessInfo.class);
		when(user.id()).thenReturn("uid");
		when(user.email()).thenReturn("e@example.com");
		when(user.firstName()).thenReturn("First");
		when(user.lastName()).thenReturn("Last");
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@example.com"))).thenReturn(List.of(user));
		// Verify email call
		doReturn(ResponseEntity.noContent().build()).when(keyCloakUserClient)
				.verifyEmail(eq("realm"), anyString(), eq("uid"), any(VerifyParam.class));
		// Build success email
		when(emailTemplateService.buildEmailVerificationSuccess(anyString(), anyString())).thenReturn("html");
		// Password flow
		when(redisService.getValue("password:e@example.com", String.class)).thenReturn("enc");
		when(aesSecretKey.decrypt("enc")).thenReturn("plain");
		doNothing().when(redisService).delete("otp:e@example.com");
		doNothing().when(redisService).delete("password:e@example.com");
		// Login path stubs
		UserAccessInfo verifiedUser = mock(UserAccessInfo.class);
		when(verifiedUser.emailVerified()).thenReturn(true);
		when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@example.com"))).thenReturn(List.of(verifiedUser));
		KeyCloakTokenResponse tokenResponse = mock(KeyCloakTokenResponse.class);
		when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenReturn(tokenResponse);

		KeyCloakTokenResponse result = authService.verifyEmail("e@example.com", "123456");
		assertEquals(tokenResponse, result);
	}

	@Test
	void verifyResetToken_invalidToken_throwsBadRequest() {
		AuthServiceImpl spyService = spy(authService);
		doThrow(new BadRequestException(
				com.fptu.sep490.identityservice.constants.Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
				com.fptu.sep490.identityservice.constants.Constants.ErrorCode.INVALID_VERIFIED_TOKEN))
				.when(spyService).isValidToken(anyString(), anyString(), anyString());
		assertThrows(BadRequestException.class, () -> spyService.verifyResetToken("e@example.com", "otp"));
	}

	@Test
	void verifyResetToken_validToken_noException() {
		AuthServiceImpl spyService = spy(authService);
		doReturn(true).when(spyService).isValidToken(anyString(), anyString(), anyString());
		assertDoesNotThrow(() -> spyService.verifyResetToken("e@example.com", "otp"));
	}

	@Test
	void checkResetPasswordToken_invalidToken_throwsBadRequest() {
		AuthServiceImpl spyService = spy(authService);
		doThrow(new BadRequestException(
				com.fptu.sep490.identityservice.constants.Constants.ErrorCodeMessage.INVALID_VERIFIED_TOKEN,
				com.fptu.sep490.identityservice.constants.Constants.ErrorCode.INVALID_VERIFIED_TOKEN))
				.when(spyService).isValidCheckedToken(anyString(), anyString(), anyString());
		assertThrows(BadRequestException.class, () -> spyService.checkResetPasswordToken("e@example.com", "otp"));
	}

	@Test
	void checkResetPasswordToken_validToken_noException() {
		AuthServiceImpl spyService = spy(authService);
		doReturn(true).when(spyService).isValidCheckedToken(anyString(), anyString(), anyString());
		assertDoesNotThrow(() -> spyService.checkResetPasswordToken("e@example.com", "otp"));
	}

	@Test
	void changePassword_wrongOldPassword_throwsAppException() throws Exception {
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			FeignException fe = mock(FeignException.class);
			when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenThrow(fe);
			assertThrows(AppException.class, () -> authService.changePassword("access", new PasswordChange("old","n","n")));
		}
	}

	@Test
	void changePassword_mismatchNewConfirm_throwsConflict() throws Exception {
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			KeyCloakTokenResponse tr = mock(KeyCloakTokenResponse.class);
			when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenReturn(tr);
			assertThrows(ConflictException.class, () -> authService.changePassword("access", new PasswordChange("old","a","b")));
		}
	}

	@Test
	void changePassword_userNotFound_throwsNotFound() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			KeyCloakTokenResponse tr = mock(KeyCloakTokenResponse.class);
			when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenReturn(tr);
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of());
			assertThrows(NotFoundException.class, () -> authService.changePassword("access", new PasswordChange("old","n","n")));
		}
	}

	@Test
	void changePassword_success() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			KeyCloakTokenResponse tr = mock(KeyCloakTokenResponse.class);
			when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenReturn(tr);
			UserAccessInfo user = mock(UserAccessInfo.class);
			when(user.id()).thenReturn("uid");
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
			doReturn(ResponseEntity.noContent().build()).when(keyCloakUserClient)
					.resetPassword(eq("realm"), anyString(), eq("uid"), any(ChangePasswordRequest.class));
			assertDoesNotThrow(() -> authService.changePassword("access", new PasswordChange("old","n","n")));
		}
	}

	@Test
	void updateUserProfile_userNotFound_throwsAppException() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of());
			assertThrows(AppException.class, () -> authService.updateUserProfile("access", new UserUpdateRequest("F","L")));
		}
	}

	@Test
	void updateUserProfile_keycloakReturnsNotNoContent_throwsInternalServerError() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo user = mock(UserAccessInfo.class);
			when(user.id()).thenReturn("uid");
			when(user.email()).thenReturn("e@x.com");
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
			ResponseEntity<Void> resp = new ResponseEntity<>(null, org.springframework.http.HttpStatus.OK);
			doReturn(resp).when(keyCloakUserClient).updateUserProfile(eq("realm"), anyString(), eq("uid"), anyMap());
			assertThrows(InternalServerErrorException.class, () -> authService.updateUserProfile("access", new UserUpdateRequest("F","L")));
		}
	}

	@Test
	void updateUserProfile_feignException_normalizedToAppException() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo user = mock(UserAccessInfo.class);
			when(user.id()).thenReturn("uid");
			when(user.email()).thenReturn("e@x.com");
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
			FeignException fe = mock(FeignException.class);
			when(keyCloakUserClient.updateUserProfile(eq("realm"), anyString(), eq("uid"), anyMap())).thenThrow(fe);
			when(errorNormalizer.handleKeyCloakException(fe)).thenThrow(new AppException("err","err",400));
			assertThrows(AppException.class, () -> authService.updateUserProfile("access", new UserUpdateRequest("F","L")));
		}
	}

	@Test
	void updateUserProfile_success_returnsUpdatedProfile() throws Exception {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo user = mock(UserAccessInfo.class);
			when(user.id()).thenReturn("uid");
			when(user.email()).thenReturn("e@x.com");
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("e@x.com"))).thenReturn(List.of(user));
			when(keyCloakUserClient.updateUserProfile(eq("realm"), anyString(), eq("uid"), anyMap())).thenReturn(ResponseEntity.noContent().build());

			UserCreationProfile res = authService.updateUserProfile("access", new UserUpdateRequest("First","Last"));
			assertEquals("uid", res.id());
			assertEquals("e@x.com", res.email());
			assertEquals("First", res.firstName());
			assertEquals("Last", res.lastName());
		}
	}

	@Test
	void createGoogleUrl_buildsCorrectly() {
		String url = authService.createGoogleUrl();
		assertTrue(url.startsWith("http://kc/realms/realm/protocol/openid-connect/auth"));
		assertTrue(url.contains("client_id=client"));
		assertTrue(url.contains("redirect_uri=http%3A%2F%2Fredirect"));
		assertTrue(url.contains("response_type=code"));
		assertTrue(url.contains("scope=openid"));
		assertTrue(url.contains("kc_idp_hint=google"));
	}

	@Test
	void getUserProfile_cacheHit_returnsCached() throws Exception {
		UserProfileMappingRoles cached = UserProfileMappingRoles.builder()
				.email("cached@example.com").build();
		when(redisService.getValue(eq(Constants.RedisKey.PROFILE + "user@example.com"), eq(UserProfileMappingRoles.class)))
				.thenReturn(cached);
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);
			UserProfileMappingRoles res = authService.getUserProfile("access");
			assertEquals("cached@example.com", res.email());
		}
	}

	@Test
	void getUserProfile_cacheMiss_buildsAndCaches() throws Exception {
		when(redisService.getValue(anyString(), eq(UserProfileMappingRoles.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			UserAccessInfo user = mock(UserAccessInfo.class);
			when(user.id()).thenReturn("uid");
			when(user.email()).thenReturn("user@example.com");
			when(user.firstName()).thenReturn("First");
			when(user.lastName()).thenReturn("Last");
			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("user@example.com"))).thenReturn(List.of(user));
			java.util.List<RoleMappingResponse> defaults = new java.util.ArrayList<>(java.util.List.of(new RoleMappingResponse("1","ROLE_USER","",false,false,"c")));
			when(keyCloakUserClient.getDefaultRole(eq("realm"), anyString())).thenReturn(defaults);
			KeyCloakRoleResponse roles = new KeyCloakRoleResponse(List.of(new RoleMappingResponse("2","ROLE_ADMIN","",false,false,"c")));
			when(keyCloakUserClient.getUserRoleMappings(eq("realm"), anyString(), eq("uid"))).thenReturn(roles);

			UserProfileMappingRoles res = authService.getUserProfile("access");
			assertEquals("user@example.com", res.email());
			assertTrue(res.roles().contains("ROLE_USER"));
			assertTrue(res.roles().contains("ROLE_ADMIN"));
			verify(redisService).saveValue(eq(Constants.RedisKey.PROFILE + "user@example.com"), any(UserProfileMappingRoles.class));
			verify(redisService).setTTL(eq(Constants.RedisKey.PROFILE + "user@example.com"), any());
		}
	}

	@Test
	void getUserProfile_userNotFound_throwsNotFound() throws Exception {
		when(redisService.getValue(anyString(), eq(UserProfileMappingRoles.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaim("preferred_username")).thenReturn("user@example.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			when(keyCloakUserClient.getUserByEmail(eq("realm"), anyString(), eq("user@example.com"))).thenReturn(List.of());
			assertThrows(NotFoundException.class, () -> authService.getUserProfile("access"));
		}
	}

	// ===== Tests for isValidCheckedToken =====
	@Test
	void isValidCheckedToken_cacheMiss_throwsBadRequest() throws JsonProcessingException {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = "dummy";
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		when(redisService.getValue(eq("verify-token:reset-password:" + email), eq(String.class))).thenReturn(null);
		assertThrows(BadRequestException.class, () -> spyService.isValidCheckedToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidCheckedToken_expired_throwsTimeout() throws JsonProcessingException {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("reset-password", "reset-password", java.time.Instant.now().minusSeconds(60), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		when(redisService.getValue(eq("verify-token:reset-password:" + email), eq(String.class))).thenReturn(token);
		assertThrows(BadRequestException.class, () -> spyService.isValidCheckedToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidCheckedToken_actionMismatch_throwsInvalid() throws JsonProcessingException {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("other-action", "reset-password", java.time.Instant.now().plusSeconds(600), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		when(redisService.getValue(eq("verify-token:reset-password:" + email), eq(String.class))).thenReturn(token);
		assertThrows(BadRequestException.class, () -> spyService.isValidCheckedToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidCheckedToken_success_returnsTrue() throws JsonProcessingException {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("reset-password", "reset-password", java.time.Instant.now().plusSeconds(600), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		when(redisService.getValue(eq("verify-token:reset-password:" + email), eq(String.class))).thenReturn(token);
		assertTrue(spyService.isValidCheckedToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidCheckedToken_nullExpiration_throwsTimeout() throws JsonProcessingException {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyTokenNoExp("reset-password", "reset-password", "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		when(redisService.getValue(eq("verify-token:reset-password:" + email), eq(String.class))).thenReturn(token);
		assertThrows(BadRequestException.class, () -> spyService.isValidCheckedToken(token, "reset-password", "reset-password"));
	}

	private String buildVerifyTokenNoExp(String action, String purpose, String userId, String email) {
		String secret = (String) org.springframework.test.util.ReflectionTestUtils.getField(authService, "emailVerifySecret");
		String issuer = (String) org.springframework.test.util.ReflectionTestUtils.getField(authService, "issuerUri");
		java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return io.jsonwebtoken.Jwts.builder()
				.setSubject(userId)
				.claim("username", email)
				.claim("email", email)
				.claim("action", action)
				.claim("purpose", purpose)
				.setIssuedAt(java.util.Date.from(java.time.Instant.now()))
				.setIssuer(issuer)
				.signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
				.compact();
	}

	private String buildVerifyToken(String action, String purpose, java.time.Instant expiration, String userId, String email) {
		String secret = (String) org.springframework.test.util.ReflectionTestUtils.getField(authService, "emailVerifySecret");
		String issuer = (String) org.springframework.test.util.ReflectionTestUtils.getField(authService, "issuerUri");
		java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return io.jsonwebtoken.Jwts.builder()
				.setSubject(userId)
				.claim("username", email)
				.claim("email", email)
				.claim("action", action)
				.claim("purpose", purpose)
				.setIssuedAt(java.util.Date.from(java.time.Instant.now()))
				.setExpiration(java.util.Date.from(expiration))
				.setIssuer(issuer)
				.signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
				.compact();
	}

	// ===== Tests for isValidToken =====
	@Test
	void isValidToken_cacheMiss_throwsBadRequest() {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = "any";
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		doReturn(false).when(spyService).isTokenValidInCache(eq(email), eq("reset-password"), eq(token));
		assertThrows(BadRequestException.class, () -> spyService.isValidToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidToken_expired_throwsTimeout() {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("reset-password", "reset-password", java.time.Instant.now().minusSeconds(60), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		doReturn(true).when(spyService).isTokenValidInCache(eq(email), eq("reset-password"), eq(token));
		assertThrows(BadRequestException.class, () -> spyService.isValidToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidToken_actionMismatch_throwsInvalid() {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("other-action", "reset-password", java.time.Instant.now().plusSeconds(600), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		doReturn(true).when(spyService).isTokenValidInCache(eq(email), eq("reset-password"), eq(token));
		assertThrows(BadRequestException.class, () -> spyService.isValidToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidToken_malformedToken_throwsBadRequest() {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = "not-a-jwt";
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		doReturn(true).when(spyService).isTokenValidInCache(eq(email), eq("reset-password"), eq(token));
		assertThrows(BadRequestException.class, () -> spyService.isValidToken(token, "reset-password", "reset-password"));
	}

	@Test
	void isValidToken_success_deletesCacheAndReturnsTrue() {
		AuthServiceImpl spyService = spy(authService);
		String email = "e@x.com";
		String token = buildVerifyToken("reset-password", "reset-password", java.time.Instant.now().plusSeconds(600), "uid", email);
		doReturn(email).when(spyService).getEmailFromToken(anyString());
		doReturn(true).when(spyService).isTokenValidInCache(eq(email), eq("reset-password"), eq(token));
		assertTrue(spyService.isValidToken(token, "reset-password", "reset-password"));
		verify(redisService).delete(eq("verify-token:reset-password:" + email));
	}

	@Test
	void getEmailFromToken_success_returnsEmail() {
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			Jwt jwt = mock(Jwt.class);
			when(jwt.getClaimAsString("email")).thenReturn("e@x.com");
			when(decoder.decode(anyString())).thenReturn(jwt);
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			String email = authService.getEmailFromToken("access");
			assertEquals("e@x.com", email);
		}
	}

	@Test
	void getEmailFromToken_decodeError_throwsBadRequest() {
		try (MockedStatic<JwtDecoders> decoders = mockStatic(JwtDecoders.class)) {
			JwtDecoder decoder = mock(JwtDecoder.class);
			when(decoder.decode(anyString())).thenThrow(new io.jsonwebtoken.JwtException("bad"));
			decoders.when(() -> JwtDecoders.fromIssuerLocation("issuer")).thenReturn(decoder);

			assertThrows(BadRequestException.class, () -> authService.getEmailFromToken("bad"));
		}
	}

	@Test
	void getCachedClientToken_cacheHit_returnsCached() throws JsonProcessingException {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class)))
				.thenReturn("cached-token");
		assertEquals("cached-token", authService.getCachedClientToken());
	}

	@Test
	void getCachedClientToken_cacheMiss_requestsAndCaches() throws JsonProcessingException {
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class)))
				.thenReturn(null);
		KeyCloakTokenResponse tr = mock(KeyCloakTokenResponse.class);
		when(tr.accessToken()).thenReturn("new-token");
		when(tr.expiresIn()).thenReturn(120);
		when(keyCloakTokenClient.requestToken(any(), eq("realm"))).thenReturn(tr);
		doNothing().when(redisService).saveValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq("new-token"), any());
		String result = authService.getCachedClientToken();
		assertEquals("new-token", result);
		verify(redisService).saveValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq("new-token"), any());
	}


}
