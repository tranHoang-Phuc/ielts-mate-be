package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.QuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UpdatedQuestionResponse;
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
@RequestMapping("/api/v1/questions")
public class QuestionController {

    QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<BaseResponse<List<QuestionCreationResponse>>> createQuestion(
            @RequestBody List<QuestionCreationRequest> questionCreationRequests, HttpServletRequest request
    ) throws JsonProcessingException {
        List<QuestionCreationResponse> data = questionService.createQuestions(questionCreationRequests, request);
        BaseResponse<List<QuestionCreationResponse>> response = BaseResponse.<List<QuestionCreationResponse>>builder()
                .data(data)
                .message("Questions created successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{question-id}")
    public ResponseEntity<BaseResponse<UpdatedQuestionResponse>> updateQuestion(
            @PathVariable("question-id") String questionId,
            @RequestBody UpdatedQuestionRequest questionCreationRequest,
            HttpServletRequest request
    ) throws JsonProcessingException {
        UpdatedQuestionResponse data = questionService.updateQuestion(questionId, questionCreationRequest, request);
        BaseResponse<UpdatedQuestionResponse> response = BaseResponse.<UpdatedQuestionResponse>builder()
                .data(data)
                .message("Question updated successfully")
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
