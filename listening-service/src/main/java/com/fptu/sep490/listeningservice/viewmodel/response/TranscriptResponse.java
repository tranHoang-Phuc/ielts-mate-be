package com.fptu.sep490.listeningservice.viewmodel.response;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record TranscriptResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("language_model")
        String languageModel,

        @JsonProperty("acoustic_model")
        String acousticModel,

        @JsonProperty("language_code")
        String languageCode,

        @JsonProperty("status")
        String status,

        @JsonProperty("audio_url")
        String audioUrl,

        @JsonProperty("text")
        String text,

        @JsonProperty("confidence")
        Double confidence,

        @JsonProperty("audio_duration")
        Double audioDuration,

        @JsonProperty("punctuate")
        boolean punctuate,

        @JsonProperty("format_text")
        boolean formatText,

        @JsonProperty("dual_channel")
        boolean dualChannel,

        @JsonProperty("speaker_labels")
        boolean speakerLabels,

        @JsonProperty("topics")
        List<String> topics,

        @JsonProperty("content_safety_labels")
        Map<String, Object> contentSafetyLabels,

        @JsonProperty("iab_categories_result")
        Map<String, Object> iabCategoriesResult,

        @JsonProperty("project_id")
        Integer projectId,

        @JsonProperty("token_id")
        Integer tokenId
) {}

