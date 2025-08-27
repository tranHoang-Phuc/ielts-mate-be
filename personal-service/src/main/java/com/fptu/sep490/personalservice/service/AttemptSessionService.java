package com.fptu.sep490.personalservice.service;

import java.util.UUID;

public interface AttemptSessionService {
    
    /**
     * Register an attempt session for a user
     * @param attemptId The attempt ID
     * @param userId The user ID
     * @param sessionId The WebSocket session ID
     * @return true if registration successful, false if attempt is already being used by another session
     */
    boolean registerAttemptSession(UUID attemptId, String userId, String sessionId);
    
    /**
     * Check if an attempt is already in use by another session
     * @param attemptId The attempt ID
     * @param sessionId The current session ID
     * @return true if attempt is blocked (being used by another session)
     */
    boolean isAttemptBlocked(UUID attemptId, String sessionId);
    
    /**
     * Update session activity timestamp (for ping-pong mechanism)
     * @param attemptId The attempt ID
     * @param sessionId The session ID
     */
    void updateSessionActivity(UUID attemptId, String sessionId);
    
    /**
     * Unregister an attempt session
     * @param attemptId The attempt ID
     * @param sessionId The session ID
     */
    void unregisterAttemptSession(UUID attemptId, String sessionId);
    
    /**
     * Cleanup expired sessions
     */
    void cleanupExpiredSessions();
    
    /**
     * Get the session ID currently using an attempt
     * @param attemptId The attempt ID
     * @return session ID or null if not found
     */
    String getActiveSessionForAttempt(UUID attemptId);
}
