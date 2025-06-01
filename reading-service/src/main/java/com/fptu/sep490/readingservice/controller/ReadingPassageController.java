package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.services.GroupQuestionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import com.fptu.sep490.readingservice.Dto.AddGroupQuestionRequest;

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