package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.personalservice.model.enumeration.AttemptSessionMessageType;
import com.fptu.sep490.personalservice.service.AttemptSessionService;
import com.fptu.sep490.personalservice.viewmodel.request.AttemptSessionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AttemptWebSocketController {

    private final AttemptSessionService attemptSessionService;
    private final SimpMessageSendingOperations messagingTemplate;


    @MessageMapping("attempt.register")
    public void registerAttempt(@Payload AttemptSessionMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        UUID attemptId = message.getAttemptId();
        String userId = message.getUserId();
        
        if (sessionId == null) {
            log.error("Session ID is null, cannot register attempt");
            return;
        }
        
        log.info("Registering attempt session - attemptId: {}, userId: {}, sessionId: {}", 
                attemptId, userId, sessionId);
        
        try {
            boolean registered = attemptSessionService.registerAttemptSession(attemptId, userId, sessionId);
            
            AttemptSessionMessage response = AttemptSessionMessage.builder()
                    .messageType(registered ? AttemptSessionMessageType.SESSION_VALIDATED 
                                           : AttemptSessionMessageType.ATTEMPT_BLOCKED)
                    .attemptId(attemptId)
                    .userId(userId)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().toEpochMilli())
                    .message(registered ? "Session registered successfully" 
                                       : "Attempt is already being used by another session")
                    .build();
            
            // Send response back to the specific session
            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                        sessionId, 
                        "/queue/attempt.response", 
                        response
                );
            }
            
            // Store attempt and user info in session attributes for cleanup  
            var sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("attemptId", attemptId.toString());
                sessionAttributes.put("userId", userId);
            }
            
        } catch (Exception e) {
            log.error("Error registering attempt session", e);
            
            AttemptSessionMessage errorResponse = AttemptSessionMessage.builder()
                    .messageType(AttemptSessionMessageType.ATTEMPT_BLOCKED)
                    .attemptId(attemptId)
                    .userId(userId)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().toEpochMilli())
                    .message("Error registering session: " + e.getMessage())
                    .build();
            
            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                        sessionId, 
                        "/queue/attempt.response", 
                        errorResponse
                );
            }
        }
    }


    @MessageMapping("attempt.ping")
    public void handlePing(@Payload AttemptSessionMessage message,
                          SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        UUID attemptId = message.getAttemptId();
        
        if (sessionId == null) {
            log.error("Session ID is null, cannot handle ping");
            return;
        }
        
        try {
            // Update session activity
            attemptSessionService.updateSessionActivity(attemptId, sessionId);
            
            // Send pong response
            AttemptSessionMessage pongResponse = AttemptSessionMessage.builder()
                    .messageType(AttemptSessionMessageType.PONG)
                    .attemptId(attemptId)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().toEpochMilli())
                    .message("pong")
                    .build();
            
            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                        sessionId, 
                        "/queue/attempt.response", 
                        pongResponse
                );
            }
            
        } catch (Exception e) {
            log.error("Error handling ping for attempt session", e);
        }
    }

    @MessageMapping("attempt.unregister")
    public void unregisterAttempt(@Payload AttemptSessionMessage message,
                                 SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        UUID attemptId = message.getAttemptId();
        
        if (sessionId == null) {
            log.error("Session ID is null, cannot unregister attempt");
            return;
        }
        
        log.info("Unregistering attempt session - attemptId: {}, sessionId: {}", 
                attemptId, sessionId);
        
        try {
            attemptSessionService.unregisterAttemptSession(attemptId, sessionId);
            
            AttemptSessionMessage response = AttemptSessionMessage.builder()
                    .messageType(AttemptSessionMessageType.SESSION_VALIDATED)
                    .attemptId(attemptId)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().toEpochMilli())
                    .message("Session unregistered successfully")
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    sessionId, 
                    "/queue/attempt.response", 
                    response
            );
            
            // Remove from session attributes
            var sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.remove("attemptId");
                sessionAttributes.remove("userId");
            }
            
        } catch (Exception e) {
            log.error("Error unregistering attempt session", e);
        }
    }

    /**
     * Check if attempt is blocked by another session
     */
    @MessageMapping("attempt.check")
    public void checkAttemptStatus(@Payload AttemptSessionMessage message,
                                  SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        UUID attemptId = message.getAttemptId();
        
        if (sessionId == null) {
            log.error("Session ID is null, cannot check attempt status");
            return;
        }
        
        try {
            boolean blocked = attemptSessionService.isAttemptBlocked(attemptId, sessionId);
            String activeSession = attemptSessionService.getActiveSessionForAttempt(attemptId);
            
            AttemptSessionMessage response = AttemptSessionMessage.builder()
                    .messageType(blocked ? AttemptSessionMessageType.ATTEMPT_BLOCKED 
                                        : AttemptSessionMessageType.SESSION_VALIDATED)
                    .attemptId(attemptId)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().toEpochMilli())
                    .message(blocked ? "Attempt is blocked by session: " + activeSession 
                                    : "Attempt is available")
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    sessionId, 
                    "/queue/attempt.response", 
                    response
            );
            
        } catch (Exception e) {
            log.error("Error checking attempt status", e);
        }
    }
}
