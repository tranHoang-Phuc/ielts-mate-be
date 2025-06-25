package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/exam-attempts")
public class ExamAttemptController {

    ExamAttemptService examAttemptService;

    // lam bai thi
    @PostMapping("/{url-slug}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<CreateExamAttemptResponse>> createReadingExam(
            @PathVariable ("url-slug") String urlSlug,
            HttpServletRequest httpServletRequest
    ) throws Exception{

        CreateExamAttemptResponse createExamAttemptResponse = examAttemptService.createExamAttempt(
                urlSlug,
                httpServletRequest
        );
        return ResponseEntity.ok(
                BaseResponse.<CreateExamAttemptResponse>builder()
                        .message("Your IELTS Reading test is about to begin.")
                        .data(createExamAttemptResponse)
                        .build()
        );
    }

    //xem lai bai thi: get("/{examAttemptId}")

    //xem lich su lam bai: get("/{readingExamId}")
}
