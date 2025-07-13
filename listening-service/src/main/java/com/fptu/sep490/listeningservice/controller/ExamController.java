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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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

}
