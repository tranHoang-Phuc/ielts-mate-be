package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ExamAttemptGetDetail;
import com.fptu.sep490.readingservice.viewmodel.response.SubmittedAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/exam/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamAttemptController {

    ExamAttemptService examAttemptService;

    @PutMapping("/save/{attempt-id}")
    @Operation(
            summary = "Save exam attempt answers",
            description = "Save the current state of an exam attempt by its ID, including the user's answers."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<SubmittedAttemptResponse>> submitExamAttempt(
            @PathVariable("attempt-id") String attemptId,
            @RequestBody ExamAttemptAnswersRequest answers,
            HttpServletRequest request
    ) throws JsonProcessingException {
        SubmittedAttemptResponse response = examAttemptService.submittedExam(attemptId, answers, request);
        BaseResponse<SubmittedAttemptResponse> baseResponse = BaseResponse.<SubmittedAttemptResponse>builder()
                .data(response)
                .message("Exam attempt submitted successfully")
                .build();
        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/{url-slug}")
    @Operation(
            summary = "Create a new IELTS Reading exam attempt",
            description = "Create a new exam attempt for the specified IELTS Reading exam identified by its URL slug."
    )
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
                        .message("Your IELTS Reading exam is about to begin.")
                        .data(createExamAttemptResponse)
                        .build()
        );
    }

    //xem lai bai thi: get("/{examAttemptId}")

    @GetMapping("/{examAttemptId}")
    @Operation(
            summary = "Get exam attempt details by ID",
            description = "Retrieve the details of a specific exam attempt using its ID."
    )
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

    //xem tat ca lich su lam bai: get("/history")
    @GetMapping("/history")
    @Operation(
            summary = "Get exam history",
            description = "Retrieve the history of exam attempts made by the user, with optional filtering and pagination."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<UserGetHistoryExamAttemptResponse>>> getExamHistory(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
//            @RequestParam(value = "ieltsType", required = false) String ieltsType,
//            @RequestParam(value = "partNumber", required = false) String partNumber,
//            @RequestParam(value = "questionCategory", required = false) String questionCategory,
            @RequestParam(value="readingExamName", required = false) String readingExamName,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            HttpServletRequest request
    ) throws JsonProcessingException {

        Page<UserGetHistoryExamAttemptResponse> pageableHistory = examAttemptService.getListExamHistory(
                page-1,
                size,
                readingExamName,
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
