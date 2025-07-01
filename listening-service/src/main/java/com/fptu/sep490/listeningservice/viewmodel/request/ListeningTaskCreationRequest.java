package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record ListeningTaskCreationRequest(
        @JsonProperty("ielts_type")
        Integer ieltsType,
        @JsonProperty("part_number")
        Integer partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        String title,
        @JsonProperty("audio_file")
        MultipartFile audioFile,
        @JsonProperty("is_automatic_transcription")
        boolean isAutomaticTranscription,
        @JsonProperty("transcription")
        String transcription
) {
}
