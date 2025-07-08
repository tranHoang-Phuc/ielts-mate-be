package com.fptu.sep490.listeningservice.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.QuestionService;
import com.fptu.sep490.listeningservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UpdatedQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/questions")
public class QuestionController {
    QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create questions for a group"
            , description = "This endpoint allows teachers to create multiple questions for a specific group. " +
            "The request body should contain a list of question creation requests.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "List of question creation requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Questions created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied, only teachers can create questions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<List<QuestionCreationResponse>>> createQuestion(
            @PathVariable("group-id") String groupId,
            @RequestBody List<QuestionCreationRequest> questionCreationRequests, HttpServletRequest request
    ) throws JsonProcessingException {
        List<QuestionCreationResponse> data = questionService.createQuestions(questionCreationRequests, request);
        BaseResponse<List<QuestionCreationResponse>> response = BaseResponse.<List<QuestionCreationResponse>>builder()
                .data(data)
                .message("Questions created successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{question-id}/order")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Update the order of a question in a group",
            description = "This endpoint allows teachers to update the order of a specific question within a group. " +
                    "The request body should contain the new order information.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Order update request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied, only teachers can update question order"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<UpdatedQuestionResponse>> updateOrder(
            @PathVariable("group-id") String groupId,
            @PathVariable("question-id") String questionId,
            @RequestBody OrderUpdatedQuestionRequest orderRequest,
            HttpServletRequest request
    ) throws JsonProcessingException {
        UpdatedQuestionResponse data = questionService.updateOrder(questionId, groupId, orderRequest, request);
        BaseResponse<UpdatedQuestionResponse> response = BaseResponse.<UpdatedQuestionResponse>builder()
                .data(data)
                .message("Question updated successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{question-id}/info")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Update information of a question in a group",
            description = "This endpoint allows teachers to update the information of a specific question within a group. " +
                    "The request body should contain the new information.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Information update request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question information updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied, only teachers can update question information"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<UpdatedQuestionResponse>> updateInformation(
            @PathVariable("group-id") String groupId,
            @PathVariable("question-id") String questionId,
            @RequestBody InformationUpdatedQuestionRequest informationRequest,
            HttpServletRequest request
    ) throws JsonProcessingException {
        UpdatedQuestionResponse data = questionService.updateInformation(questionId, groupId, informationRequest, request);
        BaseResponse<UpdatedQuestionResponse> response = BaseResponse.<UpdatedQuestionResponse>builder()
                .data(data)
                .message("Question updated successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{question-id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Delete a question from a group",
            description = "This endpoint allows teachers to delete a specific question from a group.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied, only teachers can delete questions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BaseResponse<Void>> deleteQuestion(
            @PathVariable("group-id") String groupId,
            @PathVariable("question-id") String questionId, HttpServletRequest request) {
        questionService.deleteQuestion(questionId, groupId, request);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Question deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
