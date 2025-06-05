package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.AttemptService;
import com.fptu.sep490.readingservice.viewmodel.response.PassageAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    AttemptService attemptService;

    @PostMapping("/passages/{passage-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PassageAttemptResponse>> createdAttempt(
            @PathVariable("passage-id") String passageId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        PassageAttemptResponse data = attemptService.createAttempt(passageId, request);
        return new ResponseEntity<>(
                BaseResponse.<PassageAttemptResponse>builder()
                        .data(data)
                        .message("Attempt created successfully")
                        .build(),
                HttpStatus.CREATED
        );
    }
}
