package com.fptu.sep490.readingservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttemptController {

    @PostMapping("/passages/{passage-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createdAttempt(
            @PathVariable("passage-id") String passageId,
            HttpServletRequest request
    ) {
        log.info("Creating attempt for reading passage");
        return ResponseEntity.ok().build();
    }
}
