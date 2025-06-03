package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.readingservice.viewmodel.request.AddGroupQuestionRequest;
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.service.GroupQuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedPassageRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.PassageDetailResponse;
import com.fptu.sep490.readingservice.viewmodel.response.PassageGetResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/passages")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingPassageController {

    PassageService passageService;
    GroupQuestionService groupQuestionService;

    /**
     * Get a list of reading passages based on specified conditions.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @param ieltsType the type of IELTS (optional)
     * @param status the status of the passage (optional)
     * @param partNumber the part number of the passage (optional)
     * @param questionCategory the category of questions (optional)
     * @return a response entity containing a list of passages and pagination information
     * @throws JsonProcessingException if there is an error processing JSON
     */
    @GetMapping
    @Operation(
            summary = "Get list of passages by condition",
            description = "This endpoint retrieves a list of reading passages based on the specified conditions. " +
                    "It supports pagination and filtering by IELTS type, status, part number, and question category."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of passages retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    @PreAuthorize("permitAll()")
    public ResponseEntity<BaseResponse<List<PassageGetResponse>>> getListPassageByCondition(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "ieltsType", required = false) Integer ieltsType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "partNumber", required = false) Integer partNumber,
            @RequestParam(value = "questionCategory", required = false) String questionCategory
    ) throws JsonProcessingException {
        var pageablePassages = passageService.getPassages(page, size, ieltsType, status, partNumber, questionCategory);
        var pagination = Pagination.builder()
                .currentPage(pageablePassages.getNumber() + 1)
                .totalPages(pageablePassages.getTotalPages())
                .pageSize(pageablePassages.getSize())
                .totalItems((int)pageablePassages.getTotalElements())
                .hasNextPage(pageablePassages.hasNext())
                .hasPreviousPage(pageablePassages.hasPrevious())
                .build();
        BaseResponse<List<PassageGetResponse>> body = BaseResponse.<List<PassageGetResponse>>builder()
                .data(pageablePassages.getContent())
                .pagination(pagination)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
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
            @ApiResponse(responseCode = "201",
                    description = "Passage created successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = AppException.class)))
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


    @PutMapping("/{passage-id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Update an existing passage",
            description = "This endpoint allows teachers to update an existing reading passage. " +
                    "The request must contain the updated details for the passage."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Passage update request",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdatedPassageRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Passage updated successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404",
                    description = "Passage not found",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<PassageDetailResponse>> updatePassage(@Valid @RequestBody
                                                                             UpdatedPassageRequest request,
                                                                             @PathVariable ("passage-id")
                                                                             UUID passageId,
                                                                             HttpServletRequest httpServletRequest) {
        PassageDetailResponse updatedPassage = passageService.updatePassage(passageId, request, httpServletRequest);
        BaseResponse<PassageDetailResponse> body = BaseResponse.<PassageDetailResponse>builder()
                .data(updatedPassage)
                .message("Successfully updated Passage")
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }


    @GetMapping("/{passage-id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Get passage by ID",
            description = "This endpoint retrieves a reading passage by its unique identifier (ID). " +
                    "It returns the details of the passage including its content, title, and other metadata."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Passage retrieved successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid passage ID",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404",
                    description = "Passage not found",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<PassageDetailResponse>> getPassageById(@PathVariable("passage-id")
                                                                              UUID passageId) {
        PassageDetailResponse passageDetail = passageService.getPassageById(passageId);
        BaseResponse<PassageDetailResponse> body = BaseResponse.<PassageDetailResponse>builder()
                .data(passageDetail)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }
    @PostMapping("/{passageId}/groups")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Add a group of questions to a passage",
            description = "Allows a TEACHER to add a group of questions to a specific reading passage."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Group question creation request",
            required = true,
            content = @Content(schema = @Schema(implementation = AddGroupQuestionRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group question added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404", description = "Passage not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<AddGroupQuestionResponse>> addGroupQuestion(
            @PathVariable("passageId") String passageId,
            @RequestBody AddGroupQuestionRequest request,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        AddGroupQuestionResponse response = groupQuestionService.createGroupQuestion(passageId, request, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<AddGroupQuestionResponse>builder()
                        .message("Add Group Question")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{passageId}/groups")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get all passages for teacher",
            description = "This endpoint retrieves all reading passages available for teachers."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Passages retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<List<AddGroupQuestionResponse>>> getAllQuestionsGroupsForTeacher(
            @PathVariable("passageId") String passageId,
            HttpServletRequest httpServletRequest

    ) throws Exception {
        // This method should return all question groups for teacher
        // Implementation is not provided in the original code, so returning an empty list for now
        List<AddGroupQuestionResponse> response = groupQuestionService.getAllQuestionsGroupsOfPassages(passageId, httpServletRequest);
        BaseResponse<List<AddGroupQuestionResponse>> body = BaseResponse.<List<AddGroupQuestionResponse>>builder()
                .data(response)
                .message("Successfully retrieved all question groups")
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @PutMapping("/{passageId}/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Update a group of questions in a passage",
            description = "Allows a TEACHER to update an existing group of questions in a specific reading passage."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Group question update request",
            required = true,
            content = @Content(schema = @Schema(implementation = AddGroupQuestionRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group question updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404", description = "Passage or group not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<AddGroupQuestionResponse>> updateGroupQuestion(
            @PathVariable("passageId") String passageId,
            @PathVariable("groupId") String groupId,
            @RequestBody AddGroupQuestionRequest request,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        AddGroupQuestionResponse response = groupQuestionService.updateGroupQuestion(passageId, groupId, request, httpServletRequest);
        return ResponseEntity.ok(
                BaseResponse.<AddGroupQuestionResponse>builder()
                        .message("Update Group Question")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{passageId}/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Delete a group of questions from a passage",
            description = "Allows a TEACHER to delete a specific group of questions from a reading passage."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Group question deleted successfully"),

            @ApiResponse(responseCode = "404", description = "Passage or group not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<Void>> deleteGroupQuestion(
            @PathVariable("passageId") String passageId,
            @PathVariable("groupId") String groupId,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        groupQuestionService.deleteGroupQuestion(passageId, groupId, httpServletRequest);

        return ResponseEntity.ok(
                BaseResponse.<Void>builder()
                        .message("Delete group question successfully")
                        .data(null)
                        .build()
        );
    }

    @GetMapping("/{passageId}/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get a specific group of questions from a passage",
            description = "Allows a TEACHER to retrieve a specific group of questions from a reading passage."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group question retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Passage or group not found", content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<AddGroupQuestionResponse>> getGroupQuestionById(
            @PathVariable("passageId") String passageId,
            @PathVariable("groupId") String groupId,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        AddGroupQuestionResponse response = groupQuestionService.getAllQuestionsGroupsOfPassages(passageId, httpServletRequest)
                .stream()
                .filter(group -> group.groupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new AppException("Group not found", "GROUP_NOT_FOUND", HttpStatus.NOT_FOUND.value()));

        return ResponseEntity.ok(
                BaseResponse.<AddGroupQuestionResponse>builder()
                        .message("Get Group Question")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{passage-id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Delete a passage",
            description = "This endpoint allows teachers to delete a reading passage by its unique identifier (ID). " +
                    "Once deleted, the passage cannot be recovered."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Passage deleted successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid passage ID",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "404",
                    description = "Passage not found",
                    content = @Content(schema = @Schema(implementation = AppException.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = AppException.class)))
    })
    public ResponseEntity<BaseResponse<Void>> deletePassage(@PathVariable("passage-id") UUID passageId) {
        passageService.deletePassage(passageId);
        BaseResponse<Void> body = BaseResponse.<Void>builder()
                .data(null)
                .message("Successfully deleted Passage")
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }




}