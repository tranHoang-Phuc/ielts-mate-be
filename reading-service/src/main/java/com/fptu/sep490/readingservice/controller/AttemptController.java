package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.AttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttemptController {

    AttemptService attemptService;

    @PostMapping("/passages/{passage-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<AttemptResponse>> createdAttempt(
            @PathVariable("passage-id") String passageId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        AttemptResponse data = attemptService.createAttempt(passageId, request);
        return new ResponseEntity<>(
                BaseResponse.<AttemptResponse>builder()
                        .data(data)
                        .message("Attempt created successfully")
                        .build(),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/save/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> saveAttempt(
            @PathVariable("attempt-id") String attemptId,
            HttpServletRequest request,
            @RequestBody SavedAnswersRequestList answers
    ) {
        attemptService.saveAttempt(attemptId, request, answers);
        return ResponseEntity.ok(BaseResponse.<Void>builder().data(null)
                .message("Save attempt successfully")
                .build());
    }

    @GetMapping("/load/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<UserDataAttempt>> loadAttempt(
            @PathVariable("attempt-id") String attemptId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        UserDataAttempt data = attemptService.loadAttempt(attemptId, request);
        return ResponseEntity.ok(BaseResponse.<UserDataAttempt>builder()
                .data(data)
                .message(null)
                .build());
    }

    @PutMapping("/submit/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<SubmittedAttemptResponse>> submitAttempt(
            @PathVariable("attempt-id") String attemptId,
            HttpServletRequest request,
            @RequestBody SavedAnswersRequestList answers
    ) throws JsonProcessingException {
        SubmittedAttemptResponse data = attemptService.submitAttempt(attemptId, request, answers);
        return ResponseEntity.ok(BaseResponse.<SubmittedAttemptResponse>builder()
                .data(data)
                .message("Submit attempt successfully")
                .build());
    }

}
