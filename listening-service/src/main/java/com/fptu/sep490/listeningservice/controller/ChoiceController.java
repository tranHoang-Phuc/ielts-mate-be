package com.fptu.sep490.listeningservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.ChoiceService;
import com.fptu.sep490.listeningservice.viewmodel.request.ChoiceRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ChoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ChoiceController {
    ChoiceService choiceService;


    @PostMapping("/questions/{question-id}/choices")
    @Operation(
            summary = "Create a new choice for a question",
            description = "Create a new choice associated with a specific question by its ID."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChoiceResponse>> createChoice(
            @PathVariable("question-id") String questionId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChoiceRequest choiceRequest,
            HttpServletRequest request) throws Exception {
        ChoiceResponse createdChoice = choiceService.createChoice(questionId, choiceRequest, request);
        BaseResponse<ChoiceResponse> response = BaseResponse.<ChoiceResponse>builder()
                .data(createdChoice)
                .message("Choice created successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/questions/{question-id}/choices/{choice-id}")
    @Operation(
            summary = "Update an existing choice for a question",
            description = "Update an existing choice associated with a specific question by its ID."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChoiceResponse>> updateChoice(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id") String choiceId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChoiceRequest choiceRequest,
            HttpServletRequest request) throws Exception {
        ChoiceResponse updatedChoice = choiceService.updateChoice(questionId, choiceId, choiceRequest, request);
        BaseResponse<ChoiceResponse> response = BaseResponse.<ChoiceResponse>builder()
                .data(updatedChoice)
                .message("Choice updated successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{question-id}/choices/{choice-id}")
    @Operation(
            summary = "Get a choice by its ID",
            description = "Retrieve a specific choice associated with a question by its ID."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChoiceResponse>> getChoiceById(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id") String choiceId,
            HttpServletRequest request) throws Exception {
        ChoiceResponse choice = choiceService.getChoiceById(questionId, choiceId, request);
        BaseResponse<ChoiceResponse> response = BaseResponse.<ChoiceResponse>builder()
                .data(choice)
                .message("Choice retrieved successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{question-id}/choices")
    @Operation(
            summary = "Get all choices of a question",
            description = "Retrieve all choices associated with a specific question by its ID."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<ChoiceResponse>>> getChoicesByQuestionId(
            @PathVariable("question-id") String questionId,
            HttpServletRequest request
    ) throws Exception {
        List<ChoiceResponse> choices = choiceService.getAllChoicesOfQuestion(questionId, request);
        BaseResponse<List<ChoiceResponse>> response = BaseResponse.<List<ChoiceResponse>>builder()
                .data(choices)
                .message("Choices retrieved successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/questions/{question-id}/choices/{choice-id}")
    @Operation(
            summary = "Delete a choice for a question",
            description = "Delete a specific choice associated with a question by its ID."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deleteChoice(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id") String choiceId,
            HttpServletRequest request) throws Exception {
        choiceService.deleteChoice(questionId, choiceId, request);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Choice deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }


    // this api for switch order of 2 choices
    @PutMapping("/questions/{question-id}/choices/switch/{choice-id-1}/{choice-id-2}")
    @Operation(
            summary = "Switch the order of two choices",
            description = "Switch the order of two choices associated with a specific question by their IDs."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> switchChoicesOrder(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id-1") String choiceId1,
            @PathVariable("choice-id-2") String choiceId2,
            HttpServletRequest request) throws Exception {
        choiceService.switchChoicesOrder(questionId, choiceId1, choiceId2, request);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Choices order switched successfully")
                .build();
        return ResponseEntity.ok(response);
    }



}
