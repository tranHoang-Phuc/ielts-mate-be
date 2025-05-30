package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passages")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingPassageController {

    @GetMapping
    public ResponseEntity<BaseResponse<String>> test() {
        return ResponseEntity.ok(
                BaseResponse.<String>builder()
                        .message("Reading Passage Service is running")
                        .data("Hello, World!")
                        .build()
        );
    }
}
