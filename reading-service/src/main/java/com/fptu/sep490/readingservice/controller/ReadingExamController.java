package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.readingservice.service.ReadingExamService;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import com.fptu.sep490.readingservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fptu.sep490.readingservice.model.ReadingExam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reading Exams", description = "Endpoints for managing reading exams")
@RestController
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/reading-exams")
public class ReadingExamController {

    ReadingExamService readingExamService;

    @PostMapping("")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Create a new reading exam",
            description = "Allows a teacher to create a new reading exam."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reading exam creation request",
            required = true,
            content = @Content(schema = @Schema(implementation = ReadingExamCreationRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<ReadingExamResponse>> createReadingExam(
            @Valid @org.springframework.web.bind.annotation.RequestBody ReadingExamCreationRequest readingExamCreationRequest,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        ReadingExamResponse response = readingExamService.createReadingExam(readingExamCreationRequest, httpServletRequest);
        BaseResponse<ReadingExamResponse> body = BaseResponse.<ReadingExamResponse>builder()
                .message("Add Group Question")
                .data(response)
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }

    @PutMapping("/{readingExamId}")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Update a reading exam",
            description = "Allows a teacher to update an existing reading exam."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reading exam update request",
            required = true,
            content = @Content(schema = @Schema(implementation = ReadingExamCreationRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<ReadingExamResponse>> updateReadingExam(
            @PathVariable("readingExamId") String readingExamId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ReadingExamCreationRequest readingExamCreationRequest,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        ReadingExamResponse response = readingExamService.updateReadingExam(readingExamId, readingExamCreationRequest, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Update Group Question")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{readingExamId}")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Get a reading exam",
            description = "Retrieve a reading exam by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam found"),
            @ApiResponse(responseCode = "404", description = "Exam not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<ReadingExamResponse>> getReadingExam(
            @PathVariable("readingExamId") String readingExamId,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        ReadingExamResponse response = readingExamService.getReadingExam(readingExamId, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Get Reading Exam")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{readingExamId}")
    @PreAuthorize("hasAnyRole('CREATOR', 'TEACHER')")
    @Operation(
            summary = "Delete a reading exam",
            description = "Delete a reading exam by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Exam not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<ReadingExamResponse>> deleteReadingExam(
            @PathVariable("readingExamId") String readingExamId,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        ReadingExamResponse response = readingExamService.deleteReadingExam(readingExamId, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Delete Group Question")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/my-exams")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Get all reading exams for creator",
            description = "Retrieve all reading exams created by the current user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of exams"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<List<ReadingExamResponse>>> getAllReadingExamsForCreator(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        Page<ReadingExamResponse> examPage = readingExamService.getAllReadingExamsForCreator(
                httpServletRequest, page -1, size, sortBy, sortDirection
        );

        Pagination pagination = Pagination.builder()
                .currentPage(examPage.getNumber() + 1)
                .totalPages(examPage.getTotalPages())
                .pageSize(examPage.getSize())
                .totalItems((int)examPage.getTotalElements())
                .hasNextPage(examPage.hasNext())
                .hasPreviousPage(examPage.hasPrevious())
                .build();

        BaseResponse<List<ReadingExamResponse>> body = BaseResponse.<List<ReadingExamResponse>>builder()
                .message("Get All Reading Exams")
                .data(examPage.getContent())
                .pagination(pagination)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping("/active-exams")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all active reading exams",
            description = "Retrieve all active reading exams (for creator)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active exams"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<List<ReadingExamResponse>>> getAllActiveReadingExams(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        Page<ReadingExamResponse> response = readingExamService.getAllActiveReadingExams(
                httpServletRequest, page-1, size, sortBy, sortDirection, keyword
        );

        Pagination pagination = Pagination.builder()
                .currentPage(response.getNumber() + 1)
                .totalPages(response.getTotalPages())
                .pageSize(response.getSize())
                .totalItems((int) response.getTotalElements())
                .hasNextPage(response.hasNext())
                .hasPreviousPage(response.hasPrevious())
                .build();

        BaseResponse<List<ReadingExamResponse>> baseResponse = BaseResponse.<List<ReadingExamResponse>>builder()
                .data(response.getContent())
                .pagination(pagination)
                .message("Active reading exams retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }


    @GetMapping("")
    @PreAuthorize("hasAnyRole('CREATOR', 'USER')")
    @Operation(
            summary = "Get all reading exams",
            description = "Retrieve all reading exams (for creator)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of exams"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<List<ReadingExamResponse>>> getAllReadingExams(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        Page<ReadingExamResponse> response = readingExamService.getAllReadingExams(
                httpServletRequest, page-1, size, sortBy, sortDirection, keyword
        );

        Pagination pagination = Pagination.builder()
                .currentPage(response.getNumber() + 1)
                .totalPages(response.getTotalPages())
                .pageSize(response.getSize())
                .totalItems((int) response.getTotalElements())
                .hasNextPage(response.hasNext())
                .hasPreviousPage(response.hasPrevious())
                .build();

        BaseResponse<List<ReadingExamResponse>> baseResponse = BaseResponse.<List<ReadingExamResponse>>builder()
                .data(response.getContent())
                .pagination(pagination)
                .message("Active listening exams retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);


    }

    @GetMapping("/internal/exam")
    @Operation(
            summary = "Get task titles by IDs",
            description = "Retrieve task titles for a list of task IDs."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<TaskTitle>>> getTaskTitle(@RequestParam("ids") List<UUID> ids) {
        List<TaskTitle> data = readingExamService.getTaskTitle(ids);
        BaseResponse<List<TaskTitle>> response = BaseResponse.<List<TaskTitle>>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
}