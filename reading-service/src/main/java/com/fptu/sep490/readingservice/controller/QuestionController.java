package com.fptu.sep490.readingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.QuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
