package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.ExamService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/exams")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ExamController {
    ExamService examService;


    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Create a new listening exam",
            description = "This endpoint allows creators to create a new IELTS listening exam with 4 parts. " +
                    "Each part must reference an existing ListeningTask entity."
    )
    @RequestBody(
            description = "Request body to create a listening exam",
            required = true,
            content = @Content(schema = @Schema(implementation = ExamRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Listening exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<ExamResponse>> createExam(
            @Valid @org.springframework.web.bind.annotation.RequestBody ExamRequest request,
            HttpServletRequest httpServletRequest) throws Exception {
        ExamResponse response = examService.createExam(request, httpServletRequest);
        BaseResponse<ExamResponse> baseResponse = BaseResponse.<ExamResponse>builder()
                .data(response)
                .message("Question group created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @GetMapping("/{examId}")
    @PreAuthorize("hasRole('CREATOR') or hasRole('STUDENT')")
    @Operation(
            summary = "Get a listening exam by ID",
            description = "This endpoint allows users to retrieve a specific listening exam by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listening exam retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Listening exam not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<ExamResponse>> getExamById(
            @PathVariable String examId,
            HttpServletRequest httpServletRequest) throws Exception {
        ExamResponse response = examService.getExamById(examId, httpServletRequest);
        BaseResponse<ExamResponse> baseResponse = BaseResponse.<ExamResponse>builder()
                .data(response)
                .message("Listening exam retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Delete a listening exam by ID",
            description = "This endpoint allows creators to delete a specific listening exam by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Listening exam deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Listening exam not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<Void>> deleteExam(
            @PathVariable String examId,
            HttpServletRequest httpServletRequest) throws Exception {
        examService.deleteExam(examId, httpServletRequest);
        BaseResponse<Void> baseResponse = BaseResponse.<Void>builder()
                .message("Listening exam deleted successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Update a listening exam by ID",
            description = "This endpoint allows creators to update a specific listening exam by its ID."
    )
    @RequestBody(
            description = "Request body to update a listening exam",
            required = true,
            content = @Content(schema = @Schema(implementation = ExamRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listening exam updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404", description = "Listening exam not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<ExamResponse>> updateExam(
            @PathVariable String examId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ExamRequest request,
            HttpServletRequest httpServletRequest) throws Exception {
        ExamResponse response = examService.updateExam(examId, request, httpServletRequest);
        BaseResponse<ExamResponse> baseResponse = BaseResponse.<ExamResponse>builder()
                .data(response)
                .message("Listening exam updated successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

}
