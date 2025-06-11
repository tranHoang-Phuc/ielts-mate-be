package com.fptu.sep490.readingservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.ReadingExamService;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
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
@RequestMapping("/api/v1/reading-exams")
public class ReadingExamController {

    ReadingExamService readingExamService;

    @PostMapping("/")
    @PreAuthorize("TEACHER")
    public ResponseEntity<BaseResponse<ReadingExamResponse>> createReadingExam(
            @RequestBody ReadingExamCreationRequest readingExamCreationRequest,
            HttpServletRequest httpServletRequest
    )
    throws Exception{
        ReadingExamResponse response = readingExamService.createReadingExam(readingExamCreationRequest, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Add Group Question")
                        .data(response)
                        .build()
        );
    }
    @PutMapping("/{readingExamId}")
    @PreAuthorize("TEACHER")
    public ResponseEntity<BaseResponse<ReadingExamResponse>> updateReadingExam(
            @PathVariable("readingExamId") String readingExamId,
            @RequestBody ReadingExamCreationRequest readingExamCreationRequest,
            HttpServletRequest httpServletRequest
    )
        throws Exception{
        ReadingExamResponse response = readingExamService.updateReadingExam(readingExamId,readingExamCreationRequest, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Update Group Question")
                        .data(response)
                        .build()
        );
    }
    @GetMapping("/{readingExamId}")
    @PreAuthorize("TEACHER")
    public ResponseEntity<BaseResponse<ReadingExamResponse>> getReadingExam(
            @PathVariable("readingExamId") String readingExamId,
            HttpServletRequest httpServletRequest
    ) throws Exception{
        ReadingExamResponse response= readingExamService.getReadingExam(readingExamId, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Update Group Question")
                        .data(response)
                        .build()
        );
    }

}
