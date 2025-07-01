package com.fptu.sep490.notificationservice.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface SseService {
    SseEmitter subscribe(UUID clientId);
    void sendMessage(UUID clientId, String message, String status);
}
