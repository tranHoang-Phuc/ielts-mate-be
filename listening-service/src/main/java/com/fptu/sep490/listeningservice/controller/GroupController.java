package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.GroupService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.UUID;

/**
 * Controller for handling Question Group related endpoints.
 * Provides API to create a question group for a specific listening task.
 */
@RestController
@RequestMapping("/listens")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GroupController {
    GroupService groupService;

    /**
     * Create a new question group for a given listening task.
     *
     * @param listeningTaskId the ID of the listening task
     * @param request the group creation request body
     * @param httpServletRequest the HTTP servlet request (for user context)
     * @return ResponseEntity with BaseResponse containing the created group info
     * @throws IOException if an I/O error occurs
     */
    @PostMapping(value = "/{listening-task-id}/groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a new question group for a listening task",
            description = "Create a new question group associated with a specific listening task by its ID."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<QuestionGroupResponse>> createGroup(
            @PathVariable("listening-task-id") String listeningTaskId,
            @Valid @org.springframework.web.bind.annotation.RequestBody QuestionGroupCreationRequest request,
            HttpServletRequest httpServletRequest) throws Exception {
        QuestionGroupResponse response = groupService.createGroup(listeningTaskId, request, httpServletRequest);
        BaseResponse<QuestionGroupResponse> baseResponse = BaseResponse.<QuestionGroupResponse>builder()
                .data(response)
                .message("Question group created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    //delete group
    @DeleteMapping("/{listening-task-id}/groups/{group-id}")
    @Operation(
            summary = "Delete a question group by its ID",
            description = "Delete a specific question group associated with a listening task by its ID."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<QuestionGroupResponse>> deleteGroup(
            @PathVariable("listening-task-id") UUID listeningTaskId,
            @PathVariable("group-id") UUID groupId,
            HttpServletRequest httpServletRequest) throws Exception {
        groupService.deleteGroup(listeningTaskId, groupId, httpServletRequest);
        BaseResponse<QuestionGroupResponse> baseResponse = BaseResponse.<QuestionGroupResponse>builder()
                .message("Question group deleted successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

}