package com.fptu.sep490.personalservice.config;

import com.fptu.sep490.personalservice.model.enumeration.MessageType;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.viewmodel.request.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messageTemplate;
    private final AIService aiService;



    //when user left the web socket, this method will be called
    @EventListener
    public void handlerWebSocketDisconnectListener(SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if(username != null) {
            log.info("User Disconnected : " + username);
            var chatMessage = ChatMessage.builder()
                    .messageType(MessageType.LEAVE)
                    .sender(username)
                    .build();
            // Here you can add logic to handle user disconnection, like notifying other users
            messageTemplate.convertAndSend("/topic/public", chatMessage);

        } else {
            log.warn("User disconnected without a username");
        }
    }
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        System.out.println("Session disconnected: " + sessionId);

        // Clear AI session tương ứng
        aiService.clearSession(sessionId);
    }
}
