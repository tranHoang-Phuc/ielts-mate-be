package com.fptu.sep490.notificationservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.notificationservice.service.SseService;
import com.fptu.sep490.notificationservice.viewmodel.event.BaseMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service

public class SseServiceImpl implements SseService {

    private final Map<UUID, SseEmitter> clientEmitters = new ConcurrentHashMap<>();


    private final ObjectMapper objectMapper;

    public SseServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter subscribe(UUID clientId) {
        // Set timeout to 30 minutes (1800000ms) instead of infinite
        SseEmitter emitter = new SseEmitter(1800000L);
        
        // Remove any existing connection for this client
        SseEmitter existingEmitter = clientEmitters.get(clientId);
        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("Error completing existing emitter for client {}: {}", clientId, e.getMessage());
            }
        }
        
        clientEmitters.put(clientId, emitter);
        log.info("Client {} subscribed to SSE stream", clientId);

        emitter.onCompletion(() -> {
            clientEmitters.remove(clientId);
            log.info("SSE connection completed for client {}", clientId);
        });
        emitter.onTimeout(() -> {
            clientEmitters.remove(clientId);
            log.info("SSE connection timed out for client {}", clientId);
        });
        emitter.onError((e) -> {
            clientEmitters.remove(clientId);
            log.error("SSE connection error for client {}: {}", clientId, e.getMessage());
        });

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data("{\"message\":\"Connected to SSE stream\",\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message to client {}: {}", clientId, e.getMessage());
            clientEmitters.remove(clientId);
        }

        return emitter;
    }

    @Override
    public void sendMessage(UUID clientId, String message, String status) {
        SseEmitter emitter = clientEmitters.get(clientId);
        if (emitter == null) {
            log.warn("No SSE connection found for client {}", clientId);
            return;
        }
        
        BaseMessageResponse response = BaseMessageResponse.builder()
                .message(message)
                .status(status)
                .build();
        log.info("Sending message to client {}: {}", clientId, response);
        
        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("Failed to send SSE message to client {}: {}", clientId, e.getMessage());
            clientEmitters.remove(clientId);
            try {
                emitter.completeWithError(e);
            } catch (Exception completionError) {
                log.error("Error completing emitter with error for client {}: {}", clientId, completionError.getMessage());
            }
        }
    }
}
