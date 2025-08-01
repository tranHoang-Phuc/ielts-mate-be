package com.fptu.sep490.listeningservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.ChoiceService;
import com.fptu.sep490.listeningservice.viewmodel.request.ChoiceRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ChoiceResponse;
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



}
