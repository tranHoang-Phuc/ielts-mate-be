package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.identityservice.service.ForgotPasswordRateLimiter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ForgotPasswordRateLimiterImpl implements ForgotPasswordRateLimiter {

    RedisService redisService;

    @Value("${rate-limiter.max-attempts}")
    @NonFinal
    int maxAttempts;

    @Value("${rate-limiter.block-duration}")
    @NonFinal
    int duration;

    @Override
    public boolean isBlocked(String email) throws JsonProcessingException {
        String key = getKey(email);
        Integer count = redisService.getValue(key, Integer.class);
        if(count == null) {
            resetAttempts(email);
            return false;
        }
        return count != null && count >= maxAttempts;
    }
    @Override

    public void recordAttempt(String email) throws JsonProcessingException {
        String key = getKey(email);
        Integer count = redisService.getValue(key, Integer.class);

        if (count == null) {
            redisService.saveValue(key, 1, Duration.ofSeconds(duration));
        } else {
            redisService.saveValue(key, count + 1, Duration.ofSeconds(duration));
        }
    }

    public void resetAttempts(String email) {
        redisService.delete(getKey(email));
    }

    private String getKey(String email) {
        return "forgot-password:attempts:" + email;
    }
}

