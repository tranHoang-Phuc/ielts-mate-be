package com.fptu.sep490.listeningservice.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class HelperTest {

	@InjectMocks
	Helper helper;
	@Mock KeyCloakTokenClient keyCloakTokenClient;
	@Mock KeyCloakUserClient keyCloakUserClient;
	@Mock RedisService redisService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(helper, "realm", "realm");
		ReflectionTestUtils.setField(helper, "clientId", "client");
		ReflectionTestUtils.setField(helper, "clientSecret", "secret");
	}

	@Test
	void getUserProfileById_cached_returnsCachedProfile() throws JsonProcessingException {
		String userId = "u1";
		UserProfileResponse cached = UserProfileResponse.builder().id(userId).firstName("A").lastName("B").email("e@x.com").build();
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(cached);
		// ensure token retrieval uses cached token and does not call token client
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");

		UserProfileResponse res = helper.getUserProfileById(userId);

		assertEquals(cached, res);
		verify(keyCloakUserClient, never()).getUserById(anyString(), anyString(), anyString());
	}

	@Test
	void getUserProfileById_noCache_fetchesAndCaches() throws JsonProcessingException {
		String userId = "u2";
		// no cached profile
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		// cached client token available
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserProfileResponse fetched = UserProfileResponse.builder().id(userId).firstName("C").lastName("D").email("cd@x.com").build();
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer ctoken"), eq(userId))).thenReturn(fetched);

		UserProfileResponse res = helper.getUserProfileById(userId);

		assertEquals(fetched, res);
		verify(redisService).saveValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(fetched), any(Duration.class));
	}

	@Test
	void getUserProfileById_noCachedToken_requestsToken_thenFetches() throws JsonProcessingException {
		String userId = "u3";
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		// token not cached, request new
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn(null);
		when(keyCloakTokenClient.requestToken(any(LinkedMultiValueMap.class), eq("realm")))
				.thenReturn(KeyCloakTokenResponse.builder().accessToken("newtoken").expiresIn(3600).build());
		UserProfileResponse fetched = UserProfileResponse.builder().id(userId).email("u3@x.com").build();
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer newtoken"), eq(userId))).thenReturn(fetched);

		UserProfileResponse res = helper.getUserProfileById(userId);

		assertEquals(fetched, res);
		verify(redisService).saveValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq("newtoken"), any(Duration.class));
		verify(redisService).saveValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(fetched), any(Duration.class));
	}

	@Test
	void getUserProfileById_nullFromKeycloak_throwsUnauthorized() throws JsonProcessingException {
		String userId = "u4";
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer ctoken"), eq(userId))).thenReturn(null);

		AppException ex = assertThrows(AppException.class, () -> helper.getUserProfileById(userId));
		assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpStatusCode());
	}
}


