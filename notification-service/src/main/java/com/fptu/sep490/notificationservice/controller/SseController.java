package com.fptu.sep490.notificationservice.controller;

import com.fptu.sep490.notificationservice.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/sse")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class SseController {
    SseService sseService;

    @GetMapping("/stream/{user-id}")
    @Operation(
            summary = "Subscribe to SSE stream",
            description = "Establish a Server-Sent Events (SSE) connection for a specific user by their ID."
    )
    @PermitAll
    public SseEmitter stream(@PathVariable("user-id") UUID clientId) {
        log.info("SSE subscription request received for client: {}", clientId);
        return sseService.subscribe(clientId);
    }
    
    @GetMapping("/health")
    @PermitAll
    public String health() {
        return "SSE service is running";
    }

    @GetMapping("send")
    @PermitAll
    public void testSendEvent() {
        sseService.sendMessage(UUID.fromString("e2cf7176-42a3-4d32-948b-1714bbe1a0b4"), "aaaa", "b");
    }
}
