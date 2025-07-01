package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ListeningTaskCreationResponse(
        @JsonProperty("task_id")
        UUID taskId,
        @JsonProperty("ielts_type")
        Integer ieltsType,
        @JsonProperty("part_number")
        Integer partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("title")
        String title,
        @JsonProperty("audio_file_id")
        UUID audioFileId,
        @JsonProperty("transcription")
        String transcription
) {
}
