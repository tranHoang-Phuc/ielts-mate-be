package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.ChoiceService;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreation;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedChoiceRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChoiceController {
    ChoiceService choiceService;

    @GetMapping("/questions/{question-id}/choices")
    @Operation(summary = "Get all choices of a question",
            description = "Retrieve all choices associated with a specific question by its ID.")

    public ResponseEntity<BaseResponse<List<QuestionCreationResponse.ChoiceResponse>>> getChoicesByQuestionId(
            @PathVariable("question-id") String questionId) {
        List<QuestionCreationResponse.ChoiceResponse> choices = choiceService.getAllChoicesOfQuestion(questionId);
        return ResponseEntity.ok(BaseResponse.<List<QuestionCreationResponse.ChoiceResponse>>builder()
                .data(choices)
                .message(null)
                .build());
    }

    @PostMapping("/questions/{question-id}/choices")
    @Operation(
            summary = "Create a new choice for a question",
            description = "Create a new choice associated with a specific question by its ID."
    )
    public ResponseEntity<BaseResponse<QuestionCreationResponse.ChoiceResponse>> createChoice(
            @PathVariable("question-id") String questionId,
            @RequestBody ChoiceCreation choice, HttpServletRequest request) throws JsonProcessingException {
        QuestionCreationResponse.ChoiceResponse createdChoice = choiceService.createChoice(questionId, choice, request);
        BaseResponse<QuestionCreationResponse.ChoiceResponse> response = BaseResponse.<QuestionCreationResponse.ChoiceResponse>builder()
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
    public ResponseEntity<BaseResponse<QuestionCreationResponse.ChoiceResponse>> updateChoice(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id") String choiceId,
            @RequestBody UpdatedChoiceRequest choice, HttpServletRequest request) throws JsonProcessingException {
        QuestionCreationResponse.ChoiceResponse updatedChoice = choiceService.updateChoice(questionId, choiceId, choice, request);
        BaseResponse<QuestionCreationResponse.ChoiceResponse> response = BaseResponse.<QuestionCreationResponse.ChoiceResponse>builder()
                .data(updatedChoice)
                .message("Choice updated successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/questions/{question-id}/choices/{choice-id}")
    @Operation(
            summary = "Delete a choice for a question",
            description = "Delete a specific choice associated with a question by its ID."
    )
    public ResponseEntity<BaseResponse<Void>> deleteChoice(
            @PathVariable("question-id") String questionId,
            @PathVariable("choice-id") String choiceId, HttpServletRequest request) {
        choiceService.deleteChoice(questionId, choiceId, request);
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Choice deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }

}
