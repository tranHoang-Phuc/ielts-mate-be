package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
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
    ) throws JsonProcessingException {

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

    //xem tat ca lich su lam bai: get("/history")
    @GetMapping("/history")
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

}
