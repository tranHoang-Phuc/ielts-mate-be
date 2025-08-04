package com.fptu.sep490.listeningservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.listeningservice.service.ExamAttemptService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamAttemptGetDetail;
import com.fptu.sep490.listeningservice.viewmodel.response.SubmittedExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{examAttemptId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ExamAttemptGetDetail>> getExamAttemptById(
            @PathVariable("examAttemptId") String examAttemptId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        ExamAttemptGetDetail response = examAttemptService.getExamAttemptById(examAttemptId, request);
        BaseResponse<ExamAttemptGetDetail> baseResponse = BaseResponse.<ExamAttemptGetDetail>builder()
                .data(response)
                .message(null)
                .build();
        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<UserGetHistoryExamAttemptResponse>>> getExamHistory(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value= "listeningExamName", required = false) String listeningExamName,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            HttpServletRequest request
    ) throws JsonProcessingException {

        Page<UserGetHistoryExamAttemptResponse> pageableHistory = examAttemptService.getListExamHistory(
                page-1,
                size,
                listeningExamName,
                sortBy,
                sortDirection,
                request

        );
        Pagination pagination = Pagination.builder()
                .currentPage(pageableHistory.getNumber() + 1) // Convert to 1-based index
                .totalPages(pageableHistory.getTotalPages())
                .pageSize(pageableHistory.getSize())
                .totalItems((int) pageableHistory.getTotalElements())
                .hasNextPage(pageableHistory.hasNext())
                .hasPreviousPage(pageableHistory.hasPrevious())
                .build();

        BaseResponse<List<UserGetHistoryExamAttemptResponse>> response = BaseResponse.<List<UserGetHistoryExamAttemptResponse>>builder()
                .data(pageableHistory.getContent())
                .message(null)
                .pagination(pagination)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/save/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<SubmittedExamAttemptResponse>> submitExamAttempt(
            @PathVariable("attempt-id") String attemptId,
            @RequestBody ExamAttemptAnswersRequest answers,
            HttpServletRequest request
    ) throws JsonProcessingException {
        SubmittedExamAttemptResponse response = examAttemptService.submittedExam(attemptId, answers, request);
        BaseResponse<SubmittedExamAttemptResponse> baseResponse = BaseResponse.<SubmittedExamAttemptResponse>builder()
                .data(response)
                .message("Exam attempt submitted successfully")
                .build();
        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/internal/overview-progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<OverviewProgress>> getOverViewProgress(@RequestHeader("Authorization") String token, @RequestBody OverviewProgressReq body) throws JsonProcessingException {
        OverviewProgress data = examAttemptService.getOverViewProgress(body, token);
        BaseResponse<OverviewProgress> response = BaseResponse.<OverviewProgress>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
}
