package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskCreationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/listens")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ListeningTaskController {
    ListeningTaskService listeningTaskService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ListeningTaskCreationResponse>> createListeningTask(
            @RequestParam("ielts_type") Integer ieltsType,
            @RequestParam("part_number") Integer partNumber,
            @RequestParam("instruction") String instruction,
            @RequestParam("title") String title,
            @RequestPart("audio_file") MultipartFile audioFile,
            @RequestParam("is_automatic_transcription") boolean isAutomaticTranscription,
            @RequestParam(value = "transcription", required = false) String transcription,
            HttpServletRequest httpServletRequest) throws IOException {
        ListeningTaskCreationResponse response = listeningTaskService.createListeningTask(ListeningTaskCreationRequest.builder()
                        .ieltsType(ieltsType)
                        .partNumber(partNumber)
                        .instruction(instruction)
                        .title(title)
                        .audioFile(audioFile)
                        .isAutomaticTranscription(isAutomaticTranscription)
                        .transcription(transcription)
                .build(), httpServletRequest);
        BaseResponse<ListeningTaskCreationResponse> baseResponse = BaseResponse.<ListeningTaskCreationResponse>builder()
                .data(response)
                .message("Listening task created successfully")
                .build();
        return new ResponseEntity<>(baseResponse, HttpStatus.CREATED);
    }
}
