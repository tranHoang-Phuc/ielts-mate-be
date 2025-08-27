package com.fptu.sep490.personalservice.config;

import com.fptu.sep490.personalservice.service.AttemptSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduleConfig {

    private final AttemptSessionService attemptSessionService;

    /**
     * Clean up expired attempt sessions every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled cleanup of expired attempt sessions");
        attemptSessionService.cleanupExpiredSessions();
    }
}
