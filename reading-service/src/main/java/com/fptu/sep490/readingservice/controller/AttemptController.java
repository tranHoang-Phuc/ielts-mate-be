package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.readingservice.service.AttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttemptController {

    AttemptService attemptService;

    @PostMapping("/passages/{passage-id}")
    @Operation(
            summary = "Create a new attempt for a passage",
            description = "Create a new attempt associated with a specific passage by its ID."
    )
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
    @Operation(
            summary = "Save an attempt",
            description = "Save the current state of an attempt by its ID, including the user's answers."
    )
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
    @Operation(
            summary = "Load an attempt",
            description = "Load the details of an attempt by its ID, including the user's answers."
    )
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
    @Operation(
            summary = "Submit an attempt",
            description = "Submit the answers for an attempt by its ID, marking it as completed."
    )
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
    @Operation(
            summary = "Get all user attempts",
            description = "Retrieve all attempts made by the user, with optional filtering and pagination."
    )
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
            @RequestParam(value = "passageId", required = false) UUID passageId
    ) {
        List<Integer> ieltsTypeList = parseCommaSeparatedIntegers(ieltsType);
        List<Integer> partNumberList = parseCommaSeparatedIntegers(partNumber);
        List<Integer> statusList = parseCommaSeparatedIntegers(status);
        Page<UserAttemptResponse> pageableAttempt = attemptService.getAttemptByUser(page - 1, size, ieltsTypeList,statusList,
                partNumberList, sortBy, sortDirection, title, passageId, request);
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
    @Operation(
            summary = "Retrieve the result of an attempt",
            description = "Get the result of a specific attempt by its ID, including user data and answers."
    )
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

