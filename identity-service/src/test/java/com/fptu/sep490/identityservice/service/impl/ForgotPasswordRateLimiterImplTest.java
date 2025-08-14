package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ForgotPasswordRateLimiterImplTest {

	private RedisService redisService;
	private ForgotPasswordRateLimiterImpl limiter;

	@BeforeEach
	void setUp() {
		redisService = Mockito.mock(RedisService.class);
		limiter = new ForgotPasswordRateLimiterImpl(redisService);
		// Configure @Value fields
		ReflectionTestUtils.setField(limiter, "maxAttempts", 3);
		ReflectionTestUtils.setField(limiter, "duration", 60);
	}

	@Test
	void isBlocked_noCount_resetsAndReturnsFalse() throws Exception {
		String email = "user@example.com";
		when(redisService.getValue(eq("forgot-password:attempts:" + email), eq(Integer.class))).thenReturn(null);

		boolean blocked = limiter.isBlocked(email);
		assertFalse(blocked);
		verify(redisService).delete(eq("forgot-password:attempts:" + email));
	}

	@Test
	void isBlocked_belowThreshold_returnsFalse() throws Exception {
		String email = "user@example.com";
		when(redisService.getValue(eq("forgot-password:attempts:" + email), eq(Integer.class))).thenReturn(2);

		boolean blocked = limiter.isBlocked(email);
		assertFalse(blocked);
		verify(redisService, never()).delete(anyString());
	}

	@Test
	void isBlocked_atThreshold_returnsTrue() throws Exception {
		String email = "user@example.com";
		when(redisService.getValue(eq("forgot-password:attempts:" + email), eq(Integer.class))).thenReturn(3);

		boolean blocked = limiter.isBlocked(email);
		assertTrue(blocked);
	}

	@Test
	void recordAttempt_firstTime_setsToOneWithTtl() throws Exception {
		String email = "user@example.com";
		String key = "forgot-password:attempts:" + email;
		when(redisService.getValue(eq(key), eq(Integer.class))).thenReturn(null);

		limiter.recordAttempt(email);

		ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(redisService).saveValue(eq(key), eq(1), durationCaptor.capture());
		assertEquals(60, durationCaptor.getValue().getSeconds());
	}

	@Test
	void recordAttempt_increment_increasesAndResetsTtl() throws Exception {
		String email = "user@example.com";
		String key = "forgot-password:attempts:" + email;
		when(redisService.getValue(eq(key), eq(Integer.class))).thenReturn(2);

		limiter.recordAttempt(email);

		ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(redisService).saveValue(eq(key), countCaptor.capture(), durationCaptor.capture());
		assertEquals(3, countCaptor.getValue().intValue());
		assertEquals(60, durationCaptor.getValue().getSeconds());
	}

	@Test
	void resetAttempts_deletesKey() {
		String email = "user@example.com";
		limiter.resetAttempts(email);
		verify(redisService).delete(eq("forgot-password:attempts:" + email));
	}
}
