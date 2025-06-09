package com.fptu.sep490.readingservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.ReadingExamService;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamCreationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/api/v1/reading-exams")
public class ReadingExamController {

    ReadingExamService readingExamService;

    @PostMapping("/")
    @PreAuthorize("TEACHER")
    public ResponseEntity<BaseResponse<ReadingExamCreationResponse>> createReadingExam(
            @RequestBody ReadingExamCreationRequest readingExamCreationRequest,
            HttpServletRequest httpServletRequest
    )
    throws Exception{
        ReadingExamCreationResponse response = readingExamService.createReadingExam(readingExamCreationRequest, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamCreationResponse>builder()
                        .message("Add Group Question")
                        .data(response)
                        .build()
        );
    }
}
