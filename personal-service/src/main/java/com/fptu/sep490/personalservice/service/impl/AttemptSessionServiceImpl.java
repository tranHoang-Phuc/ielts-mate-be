package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.personalservice.service.AttemptSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptSessionServiceImpl implements AttemptSessionService {

    private final RedisService redisService;
    
    private static final String ATTEMPT_SESSION_PREFIX = "attempt_session:";
    private static final String SESSION_ACTIVITY_PREFIX = "session_activity:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(60); // 30 minutes
    private static final Duration ACTIVITY_TTL = Duration.ofMinutes(2); // 5 minutes for activity tracking
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofMinutes(1); // 1 minute heartbeat timeout

    @Override
    public boolean registerAttemptSession(UUID attemptId, String userId, String sessionId) {
        String attemptKey = ATTEMPT_SESSION_PREFIX + attemptId.toString();
        
        try {
            // Check if attempt is already registered to another session
            @SuppressWarnings("unchecked")
            Map<String, Object> existingSession = redisService.getValue(attemptKey, Map.class);
            
            if (existingSession != null) {
                String existingSessionId = (String) existingSession.get("sessionId");
                String existingUserId = (String) existingSession.get("userId");
                
                // If same session and user, allow (reconnection scenario)
                if (sessionId.equals(existingSessionId) && userId.equals(existingUserId)) {
                    updateSessionActivity(attemptId, sessionId);
                    return true;
                }
                
                // Check if existing session is still active
                if (isSessionActive(attemptId)) {
                    log.warn("Attempt {} is already being used by session {} for user {}", 
                            attemptId, existingSessionId, existingUserId);
                    return false;
                }
            }
            
            // Register the new session
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", sessionId);
            sessionData.put("userId", userId);
            sessionData.put("registeredAt", Instant.now().toEpochMilli());
            
            redisService.saveValue(attemptKey, sessionData, SESSION_TTL);
            updateSessionActivity(attemptId, sessionId);
            
            log.info("Registered attempt session - attemptId: {}, userId: {}, sessionId: {}", 
                    attemptId, userId, sessionId);
            return true;
            
        } catch (JsonProcessingException e) {
            log.error("Error registering attempt session", e);
            return false;
        }
    }

    @Override
    public boolean isAttemptBlocked(UUID attemptId, String sessionId) {
        String attemptKey = ATTEMPT_SESSION_PREFIX + attemptId.toString();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingSession = redisService.getValue(attemptKey, Map.class);
            
            if (existingSession == null) {
                return false;
            }
            
            String existingSessionId = (String) existingSession.get("sessionId");
            
            // If it's the same session, not blocked
            if (sessionId.equals(existingSessionId)) {
                return false;
            }
            
            // Check if existing session is still active
            return isSessionActive(attemptId);
            
        } catch (JsonProcessingException e) {
            log.error("Error checking attempt block status", e);
            return false;
        }
    }

    @Override
    public void updateSessionActivity(UUID attemptId, String sessionId) {
        String activityKey = SESSION_ACTIVITY_PREFIX + attemptId.toString();
        
        try {
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("sessionId", sessionId);
            activityData.put("lastActivity", Instant.now().toEpochMilli());
            activityData.put("lastPing", Instant.now().toEpochMilli());
            
            redisService.saveValue(activityKey, activityData, ACTIVITY_TTL);
            
        } catch (JsonProcessingException e) {
            log.error("Error updating session activity", e);
        }
    }

    @Override
    public void unregisterAttemptSession(UUID attemptId, String sessionId) {
        String attemptKey = ATTEMPT_SESSION_PREFIX + attemptId.toString();
        String activityKey = SESSION_ACTIVITY_PREFIX + attemptId.toString();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingSession = redisService.getValue(attemptKey, Map.class);
            
            if (existingSession != null) {
                String existingSessionId = (String) existingSession.get("sessionId");
                
                // Only unregister if it's the same session
                if (sessionId.equals(existingSessionId)) {
                    redisService.delete(attemptKey);
                    redisService.delete(activityKey);
                    
                    log.info("Unregistered attempt session - attemptId: {}, sessionId: {}", 
                            attemptId, sessionId);
                }
            }
            
        } catch (JsonProcessingException e) {
            log.error("Error unregistering attempt session", e);
        }
    }

    @Override
    public void cleanupExpiredSessions() {
        // This method could be enhanced to scan for expired sessions
        // For now, Redis TTL handles the cleanup automatically
        log.debug("Session cleanup triggered - Redis TTL handles automatic cleanup");
    }

    /**
     * Cleanup sessions that haven't pinged within the heartbeat timeout (1 minute)
     */
    public void cleanupInactiveHeartbeatSessions() {
        try {
            // Get all session activity keys (this would require Redis SCAN in production)
            log.info("Starting cleanup of inactive heartbeat sessions (timeout: {} ms)", HEARTBEAT_TIMEOUT.toMillis());
            
            // Note: In a production environment, you would want to use Redis SCAN command
            // to iterate through keys matching the pattern "session_activity:*"
            // For now, this method can be called when we detect inactive sessions
            
        } catch (Exception e) {
            log.error("Error during heartbeat session cleanup", e);
        }
    }

    /**
     * Check if a specific session has exceeded the heartbeat timeout
     */
    public boolean isSessionHeartbeatExpired(UUID attemptId) {
        String activityKey = SESSION_ACTIVITY_PREFIX + attemptId.toString();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> activityData = redisService.getValue(activityKey, Map.class);
            
            if (activityData == null) {
                return true; // No activity data means session is expired
            }
            
            Long lastPing = (Long) activityData.get("lastPing");
            if (lastPing == null) {
                return true; // No ping timestamp means session is expired
            }
            
            // Check if last ping was more than 1 minute ago
            long currentTime = Instant.now().toEpochMilli();
            long timeSinceLastPing = currentTime - lastPing;
            
            return timeSinceLastPing > HEARTBEAT_TIMEOUT.toMillis();
            
        } catch (JsonProcessingException e) {
            log.error("Error checking heartbeat expiration", e);
            return true; // Assume expired on error
        }
    }

    /**
     * Cleanup a specific session if its heartbeat has expired
     */
    public void cleanupExpiredHeartbeatSession(UUID attemptId) {
        if (isSessionHeartbeatExpired(attemptId)) {
            String attemptKey = ATTEMPT_SESSION_PREFIX + attemptId.toString();
            String activityKey = SESSION_ACTIVITY_PREFIX + attemptId.toString();
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = redisService.getValue(attemptKey, Map.class);
                
                if (sessionData != null) {
                    String sessionId = (String) sessionData.get("sessionId");
                    String userId = (String) sessionData.get("userId");
                    
                    // Remove from Redis
                    redisService.delete(attemptKey);
                    redisService.delete(activityKey);
                    
                    log.info("Cleaned up expired heartbeat session - attemptId: {}, userId: {}, sessionId: {}", 
                            attemptId, userId, sessionId);
                }
                
            } catch (JsonProcessingException e) {
                log.error("Error cleaning up expired heartbeat session", e);
            }
        }
    }

    @Override
    public String getActiveSessionForAttempt(UUID attemptId) {
        String attemptKey = ATTEMPT_SESSION_PREFIX + attemptId.toString();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingSession = redisService.getValue(attemptKey, Map.class);
            
            if (existingSession != null && isSessionActive(attemptId)) {
                return (String) existingSession.get("sessionId");
            }
            
            return null;
            
        } catch (JsonProcessingException e) {
            log.error("Error getting active session for attempt", e);
            return null;
        }
    }
    
    private boolean isSessionActive(UUID attemptId) {
        String activityKey = SESSION_ACTIVITY_PREFIX + attemptId.toString();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> activityData = redisService.getValue(activityKey, Map.class);
            
            if (activityData == null) {
                return false;
            }
            
            Long lastPing = (Long) activityData.get("lastPing");
            Long lastActivity = (Long) activityData.get("lastActivity");
            
            // Prioritize heartbeat ping for session validation
            if (lastPing != null) {
                long currentTime = Instant.now().toEpochMilli();
                long timeSinceLastPing = currentTime - lastPing;
                
                // Session is active if last ping was within 1 minute
                return timeSinceLastPing < HEARTBEAT_TIMEOUT.toMillis();
            }
            
            // Fallback to lastActivity if no ping data (for backward compatibility)
            if (lastActivity != null) {
                long currentTime = Instant.now().toEpochMilli();
                long timeDiff = currentTime - lastActivity;
                
                return timeDiff < ACTIVITY_TTL.toMillis();
            }
            
            return false;
            
        } catch (JsonProcessingException e) {
            log.error("Error checking session activity", e);
            return false;
        }
    }
}
