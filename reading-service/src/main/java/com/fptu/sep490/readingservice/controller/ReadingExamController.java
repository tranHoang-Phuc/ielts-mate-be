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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@Tag(name = "Reading Exams", description = "Endpoints for managing reading exams")
@RestController
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/api/v1/reading-exams")
public class ReadingExamController {

    ReadingExamService readingExamService;

    @PostMapping("/")
    @PreAuthorize("TEACHER")
    @Operation(
            summary = "Create a new reading exam",
            description = "Allows a teacher to create a new reading exam."
    )
    @RequestBody(
            required = true,
            description = "Reading exam creation request"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(
            summary = "Update a reading exam",
            description = "Allows a teacher to update an existing reading exam."
    )
    @RequestBody(
            required = true,
            description = "Reading exam update request"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    ) throws Exception{
        ReadingExamResponse response= readingExamService.getReadingExam(readingExamId, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<ReadingExamResponse>builder()
                        .message("Update Group Question")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{readingExamId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
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
    @PreAuthorize("CREATOR")
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
            HttpServletRequest httpServletRequest
    ) throws Exception {
        List<ReadingExamResponse> response = readingExamService.getAllReadingExamsForCreator(httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<List<ReadingExamResponse>>builder()
                        .message("Get All Reading Exams")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/")
    @PreAuthorize("CREATOR")
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
            HttpServletRequest httpServletRequest
    ) throws Exception {
        List<ReadingExamResponse> response = readingExamService.getAllReadingExams(httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<List<ReadingExamResponse>>builder()
                        .message("Get All Reading Exams for Creator")
                        .data(response)
                        .build()
        );
    }
}
