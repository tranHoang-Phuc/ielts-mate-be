package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.SubmittedAttemptResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/exam/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamAttemptController {

    ExamAttemptService examAttemptService;

    @PutMapping("/save/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<SubmittedAttemptResponse>> submitExamAttempt(
            @PathVariable("attempt-id") String attemptId,
            ExamAttemptAnswersRequest answers,
            HttpServletRequest request
    ) {
        SubmittedAttemptResponse response = examAttemptService.submittedExam(attemptId, answers, request);
        BaseResponse<SubmittedAttemptResponse> baseResponse = BaseResponse.<SubmittedAttemptResponse>builder()
                .data(response)
                .message("Exam attempt submitted successfully")
                .build();
        return ResponseEntity.ok(baseResponse);
    }
}
