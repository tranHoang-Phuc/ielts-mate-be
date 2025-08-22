package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AssemblyAIWebhookRequest(
        @JsonProperty("transcript_id")
        UUID transcriptId,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("text")
        String text,
        
        @JsonProperty("confidence")
        Double confidence,
        
        @JsonProperty("audio_duration")
        Double audioDuration,
        
        @JsonProperty("error")
        String error
) {
}
