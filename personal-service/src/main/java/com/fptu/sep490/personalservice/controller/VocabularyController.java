package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.personalservice.service.VocabularyService;
import com.fptu.sep490.personalservice.viewmodel.request.VocabularyRequest;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
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

import java.util.List;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/vocabulary")
@Slf4j
public class VocabularyController {
    VocabularyService vocabularyService;


    @PostMapping("/")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new vocabulary",
            description = "Create a new vocabulary with the provided details. Requires CREATOR role."
    )
    @RequestBody(
            description = "Request body to create a vocabulary",
            required = true,
            content = @Content(schema = @Schema(implementation = VocabularyRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vocabulary exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<VocabularyResponse>> createVocabulary(
            @Valid @org.springframework.web.bind.annotation.RequestBody VocabularyRequest vocabularyRequest,
            HttpServletRequest request

    ) throws Exception{
        VocabularyResponse response = vocabularyService.createVocabulary(vocabularyRequest, request);
        BaseResponse<VocabularyResponse> baseResponse = BaseResponse.<VocabularyResponse>builder()
                .data(response)
                .message("Vocabulary created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);

    }

    @PutMapping("/{vocabularyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update vocabulary by ID",
            description = "Update an existing vocabulary by its ID. Requires CREATOR role."
    )
    @RequestBody(
            description = "Request body to update a vocabulary",
            required = true,
            content = @Content(schema = @Schema(implementation = VocabularyRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vocabulary updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<VocabularyResponse>> updateVocabulary(
            @PathVariable("vocabularyId") String vocabularyId,
            @Valid @org.springframework.web.bind.annotation.RequestBody VocabularyRequest vocabularyRequest,
            HttpServletRequest request
    ) throws Exception {
        VocabularyResponse response = vocabularyService.updateVocabulary(vocabularyId, vocabularyRequest, request);
        BaseResponse<VocabularyResponse> baseResponse = BaseResponse.<VocabularyResponse>builder()
                .data(response)
                .message("Vocabulary updated successfully")
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @GetMapping("/{vocabularyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get vocabulary by ID",
            description = "Retrieve a vocabulary by its ID. Requires CREATOR role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vocabulary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<VocabularyResponse>> getVocabularyById(
            @org.springframework.web.bind.annotation.RequestParam("vocabularyId") String vocabularyId,
            HttpServletRequest request
    )throws Exception {
        VocabularyResponse response = vocabularyService.getVocabularyById(vocabularyId, request);
        BaseResponse<VocabularyResponse> baseResponse = BaseResponse.<VocabularyResponse>builder()
                .data(response)
                .message("Vocabulary get successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @DeleteMapping("/{vocabularyId}")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Delete vocabulary by ID",
            description = "Delete a vocabulary by its ID. Requires CREATOR role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vocabulary deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<Void>> deleteVocabularyById(
            @PathVariable("vocabularyId") String vocabularyId,
            HttpServletRequest request
    ) throws Exception {
        vocabularyService.deleteVocabularyById(vocabularyId, request);
        BaseResponse<Void> baseResponse = BaseResponse.<Void>builder()
                .message("Listening exam deleted successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);    }


    @GetMapping("/my-vocabulary")
    @Operation(
            summary = "Get all vocabulary of user",
            description = "Retrieve all vocabulary of user. Requires CREATOR role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vocabulary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<List<VocabularyResponse>>> getAllVocabulary(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword

    ) throws Exception {
        Page<VocabularyResponse> responses = vocabularyService.getAllVocabulary(request, page -1, size, sortBy, sortDirection, keyword);

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<VocabularyResponse>> baseResponse = BaseResponse.<List<VocabularyResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("Active listening exams retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }



}
