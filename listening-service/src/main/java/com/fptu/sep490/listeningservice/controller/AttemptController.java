package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.AttemptService;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@RestController
@RequestMapping("/attempts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AttemptController {

    AttemptService attemptService;

    @PostMapping("/{listening-task-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttemptResponse> createAttempt(@PathVariable("listening-task-id") UUID listeningTaskId,
                                                         HttpServletRequest request) {
        AttemptResponse response = attemptService.createAttempt(listeningTaskId, request);
        BaseResponse<AttemptResponse> baseResponse = BaseResponse.<AttemptResponse>builder()
                .message("Attempt created successfully")
                .data(response)
                .build();
        return new ResponseEntity<>(baseResponse, HttpStatus.CREATED);
    }
}
