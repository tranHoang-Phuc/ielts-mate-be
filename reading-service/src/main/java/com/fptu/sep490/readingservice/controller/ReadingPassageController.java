package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.services.GroupQuestionService;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import com.fptu.sep490.readingservice.Dto.AddGroupQuestionRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/passages")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingPassageController {

    GroupQuestionService groupQuestionService;
    @GetMapping
    public ResponseEntity<BaseResponse<String>> test() {
        return ResponseEntity.ok(
                BaseResponse.<String>builder()
                        .message("Reading Passage Service is running")
                        .data("Hello, World!")

                        .build()
        );
    }

    /**
     * Create a new passage.
     *
     * @param request the request containing passage creation details
     * @param httpServletRequest the HTTP servlet request
     * @return a response entity containing the created passage details
     * @throws JsonProcessingException if there is an error processing JSON
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Create a new passage",
            description = "This endpoint allows teachers to create a new reading passage. " +
                    "The request must contain the necessary details for the passage creation."
     )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Passage creation request",
            required = true,
            content = @Content(schema = @Schema(implementation = PassageCreationRequest.class)
    ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Passage created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<PassageCreationResponse>> createPassage(@Valid @RequestBody PassageCreationRequest request, HttpServletRequest httpServletRequest) throws JsonProcessingException {
        var data = passageService.createPassage(request, httpServletRequest);
        BaseResponse<PassageCreationResponse> body = BaseResponse.<PassageCreationResponse>builder()
                .data(data)
                .message("Successfully created Passage")
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);

    }


    @PostMapping("/{passageId}/groups")
    public ResponseEntity<BaseResponse<String>> addGroupQuestion(
            @PathVariable("passageId") String passageId,
            @RequestBody AddGroupQuestionRequest request
    ) {
        groupQuestionService.createGroupQuestion(passageId, request);
        return ResponseEntity.ok(
                BaseResponse.<String>builder()
                        .message("Add Group Question")
                        .data("Add Group Question")
                        .build()
        );
    }
}
