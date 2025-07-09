package com.fptu.sep490.listeningservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.listeningservice.service.AttemptService;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.SubmittedAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserDataAttempt;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attempts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AttemptController {

    AttemptService attemptService;

    @PostMapping("/{listening-task-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<AttemptResponse>> createAttempt(
            @PathVariable("listening-task-id") UUID listeningTaskId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        AttemptResponse attemptResponse = attemptService.createAttempt(listeningTaskId, request);
        BaseResponse<AttemptResponse> baseResponse = BaseResponse.<AttemptResponse>builder()
                .message("Attempt created successfully")
                .data(attemptResponse)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(baseResponse);
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

    @PutMapping("/submite/{attempt-id}")
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<UserAttemptResponse>>> getAllUserAttempt(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "ieltsType", required = false) String ieltsType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "partNumber", required = false) String partNumber,
            @RequestParam(value = "questionCategory", required = false) String questionCategory,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "listeningTaskId", required = false) UUID listeningTaskId
    ) {
        List<Integer> ieltsTypeList = parseCommaSeparatedIntegers(ieltsType);
        List<Integer> partNumberList = parseCommaSeparatedIntegers(partNumber);
        List<Integer> statusList = parseCommaSeparatedIntegers(status);
        Page<UserAttemptResponse> pageableAttempt = attemptService.getAttemptByUser(page - 1, size, ieltsTypeList,statusList,
                partNumberList, sortBy, sortDirection, title, listeningTaskId, request);
        Pagination pagination = Pagination.builder()
                .currentPage(pageableAttempt.getNumber() + 1)
                .totalPages(pageableAttempt.getTotalPages())
                .pageSize(pageableAttempt.getSize())
                .totalItems((int) pageableAttempt.getTotalElements())
                .hasNextPage(pageableAttempt.hasNext())
                .hasPreviousPage(pageableAttempt.hasPrevious())
                .build();

        BaseResponse<List<UserAttemptResponse>> body = BaseResponse.<List<UserAttemptResponse>>builder()
                .data(pageableAttempt.getContent())
                .pagination(pagination)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping("/result/{attempt-id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<UserDataAttempt>> result(
            @PathVariable("attempt-id") UUID attemptId,
            HttpServletRequest request
    ) throws JsonProcessingException {
        UserDataAttempt data = attemptService.viewResult(attemptId, request);
        BaseResponse<UserDataAttempt> response = BaseResponse.<UserDataAttempt>builder()
                .data(data)
                .message("Retrieve data successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    private List<Integer> parseCommaSeparatedIntegers(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return null;
        }
    }


}
