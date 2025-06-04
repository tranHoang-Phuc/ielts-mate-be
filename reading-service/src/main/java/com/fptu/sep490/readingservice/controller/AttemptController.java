package com.fptu.sep490.readingservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttemptController {

    @PostMapping("/passages/{passage-id}")
    public ResponseEntity<Void> createdAttempt() {
        // Logic to create an attempt for a reading passage
        log.info("Creating attempt for reading passage");
        // Placeholder for actual implementation
        return ResponseEntity.ok().build();
    }
}
