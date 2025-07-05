package com.fptu.sep490.notificationservice.controller;

import com.fptu.sep490.notificationservice.service.SseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/sse")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SseController {
    SseService sseService;

    @GetMapping("/stream/{user-id}")
    public SseEmitter stream(@PathVariable("user-id") UUID clientId) {
        return sseService.subscribe(clientId);
    }
}
