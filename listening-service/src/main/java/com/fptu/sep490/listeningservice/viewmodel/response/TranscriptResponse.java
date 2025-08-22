package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
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
        Boolean punctuate,

        @JsonProperty("format_text")
        Boolean formatText,

        @JsonProperty("dual_channel")
        Boolean dualChannel,

        @JsonProperty("speaker_labels")
        Boolean speakerLabels,

        @JsonProperty("words")
        List<WordResponse> words,

        @JsonProperty("utterances")
        List<UtteranceResponse> utterances,

        @JsonProperty("topics")
        List<String> topics,

        @JsonProperty("content_safety_labels")
        Map<String, Object> contentSafetyLabels,

        @JsonProperty("iab_categories_result")
        Map<String, Object> iabCategoriesResult,

        @JsonProperty("project_id")
        Integer projectId,

        @JsonProperty("token_id")
        Integer tokenId,

        // Additional fields that might be present
        @JsonProperty("error")
        String error,

        @JsonProperty("webhook_url")
        String webhookUrl,

        @JsonProperty("webhook_status_code")
        Integer webhookStatusCode
) {
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WordResponse(
            @JsonProperty("text")
            String text,

            @JsonProperty("start")
            Integer start,

            @JsonProperty("end")
            Integer end,

            @JsonProperty("confidence")
            Double confidence,

            @JsonProperty("speaker")
            String speaker
    ) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UtteranceResponse(
            @JsonProperty("speaker")
            String speaker,

            @JsonProperty("text")
            String text,

            @JsonProperty("confidence")
            Double confidence,

            @JsonProperty("start")
            Integer start,

            @JsonProperty("end")
            Integer end,

            @JsonProperty("words")
            List<WordResponse> words
    ) {}
}

