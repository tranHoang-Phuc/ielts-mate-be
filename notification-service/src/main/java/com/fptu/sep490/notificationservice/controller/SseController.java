package com.fptu.sep490.notificationservice.controller;

import com.fptu.sep490.notificationservice.service.SseService;
import jakarta.annotation.security.PermitAll;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/sse")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SseController {
    SseService sseService;

    @GetMapping("/stream/{user-id}")
    @PermitAll
    public SseEmitter stream(@PathVariable("user-id") UUID clientId) {
        return sseService.subscribe(clientId);
    }

    @GetMapping("abc")
    @PermitAll
    public void testSendEvent() {
        sseService.sendMessage(UUID.fromString("e2cf7176-42a3-4d32-948b-1714bbe1a0b4"), "aaaa", "b");
    }
}
