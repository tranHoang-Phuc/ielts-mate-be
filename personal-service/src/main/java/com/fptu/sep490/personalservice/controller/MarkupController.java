package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.personalservice.service.MarkupService;
import com.fptu.sep490.personalservice.viewmodel.request.MarkupCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.response.MarkUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.MarkedUpResponse;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/markup")
@Slf4j
public class MarkupController {

    MarkupService markupService;

    @PostMapping
    @Operation(
            summary = "Add a new markup",
            description = "Create a new markup for a task. The request should include the necessary details for the markup."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> addMarkup(HttpServletRequest request, MarkupCreationRequest markup) {
        markupService.addMarkup(request, markup);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Markup added successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{task-id}")
    @Operation(
            summary = "Delete a markup",
            description = "Delete an existing markup by its task ID. This operation will remove the markup from the system."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deleteMarkup(
            HttpServletRequest request, @PathVariable("task-id") UUID taskId) {
        markupService.deleteMarkup(request, taskId);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Markup deleted successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/task")
    @Operation(
            summary = "Get all markups for tasks",
            description = "Retrieve a paginated list of all markups associated with tasks. You can filter by markup type, task type, and practice type."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<MarkUpResponse>>> getAllTask(
        HttpServletRequest request,
        @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
        @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
        @RequestParam(value = "markupType", required = false) String markUpType,
        @RequestParam(value = "taskType", required = false) String taskType,
        @RequestParam(value = "practiceType", required = false) String practiceType

        ) {
        List<Integer> markupTypeList = parseCommaSeparatedIntegers(markUpType);
        List<Integer> taskTypeList = parseCommaSeparatedIntegers(taskType);
        List<Integer> practiceTypeList = parseCommaSeparatedIntegers(practiceType);

        Page<MarkUpResponse> pageableMarkup = markupService.getMarkup(page - 1, size, markupTypeList, taskTypeList, practiceTypeList, request);
        var pagination = Pagination.builder()
                .currentPage(pageableMarkup.getNumber() + 1)
                .totalPages(pageableMarkup.getTotalPages())
                .pageSize(pageableMarkup.getSize())
                .totalItems((int)pageableMarkup.getTotalElements())
                .hasNextPage(pageableMarkup.hasNext())
                .hasPreviousPage(pageableMarkup.hasPrevious())
                .build();

        BaseResponse<List<MarkUpResponse>> body = BaseResponse.<List<MarkUpResponse>>builder()
                .data(pageableMarkup.getContent())
                .pagination(pagination)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping("/internal/marked-up/{type}")
    public ResponseEntity<BaseResponse<MarkedUpResponse>> getMarkedUpData(@PathVariable("type") String type) {
        MarkedUpResponse markedUpResponse = markupService.getMarkedUpData(type);
        BaseResponse<MarkedUpResponse> response = BaseResponse.<MarkedUpResponse>builder()
                .data(markedUpResponse)
                .message("Marked up data retrieved successfully")
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
