package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TranscriptStatusResponse(
        @JsonProperty("transcript_id")
        UUID transcriptId,
        
        @JsonProperty("task_id")
        UUID taskId,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("transcript_text")
        String transcriptText,
        
        @JsonProperty("confidence")
        Double confidence,
        
        @JsonProperty("audio_duration")
        Double audioDuration,
        
        @JsonProperty("error_message")
        String errorMessage,
        
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        
        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
}
