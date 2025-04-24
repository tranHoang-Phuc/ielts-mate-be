package com.fptu.sep490.identityservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.identityservice.service.VerifyEmailRateLimiter;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class VerifyEmailRateLimiterImpl implements VerifyEmailRateLimiter {
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
        if (count == null) {
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
        return "verify-email:attempts:" + email;
    }
}
