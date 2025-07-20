package com.fptu.sep490.listeningservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.ExamAttemptService;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
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
@RequestMapping("/exam/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamAttemptController {
    ExamAttemptService examAttemptService;

    @PostMapping("/{url-slug}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<CreateExamAttemptResponse>> createReadingExam(
            @PathVariable("url-slug") String urlSlug,
            HttpServletRequest httpServletRequest
    ) throws JsonProcessingException {

        CreateExamAttemptResponse createExamAttemptResponse = examAttemptService.createExamAttempt(
                urlSlug,
                httpServletRequest
        );
        return ResponseEntity.ok(
                BaseResponse.<CreateExamAttemptResponse>builder()
                        .message("Your IELTS Listening exam is about to begin.")
                        .data(createExamAttemptResponse)
                        .build()
        );
    }
}
