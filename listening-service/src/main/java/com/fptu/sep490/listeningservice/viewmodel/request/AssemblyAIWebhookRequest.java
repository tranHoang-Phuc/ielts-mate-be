package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssemblyAIWebhookRequest(
        // The actual field name in AssemblyAI webhook is usually "id", not "transcript_id"
        @JsonProperty("id")
        UUID id,
        
        @JsonProperty("transcript_id") // Keep this as fallback
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
        String error,
        
        // Additional fields that AssemblyAI might send
        @JsonProperty("language_model")
        String languageModel,
        
        @JsonProperty("acoustic_model")
        String acousticModel,
        
        @JsonProperty("language_code")
        String languageCode,
        
        @JsonProperty("audio_url")
        String audioUrl,
        
        @JsonProperty("words")
        List<Map<String, Object>> words,
        
        @JsonProperty("utterances")
        List<Map<String, Object>> utterances,
        
        @JsonProperty("webhook_status_code")
        Integer webhookStatusCode
) {
    // Helper method to get the correct transcript ID
    public UUID getTranscriptId() {
        return id != null ? id : transcriptId;
    }
}
