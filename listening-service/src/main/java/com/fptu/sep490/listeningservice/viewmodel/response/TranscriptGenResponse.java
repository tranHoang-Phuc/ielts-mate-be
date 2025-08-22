package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record TranscriptGenResponse(
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

        @JsonProperty("words")
        List<String> words,

        @JsonProperty("utterances")
        List<String> utterances,

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

        @JsonProperty("webhook_url")
        String webhookUrl,

        @JsonProperty("webhook_status_code")
        Integer webhookStatusCode,

        @JsonProperty("webhook_auth")
        boolean webhookAuth,

        @JsonProperty("webhook_auth_header_name")
        String webhookAuthHeaderName,

        @JsonProperty("speed_boost")
        boolean speedBoost,

        @JsonProperty("auto_highlights_result")
        String autoHighlightsResult,

        @JsonProperty("auto_highlights")
        boolean autoHighlights,

        @JsonProperty("audio_start_from")
        Double audioStartFrom,

        @JsonProperty("audio_end_at")
        Double audioEndAt,

        @JsonProperty("word_boost")
        List<String> wordBoost,

        @JsonProperty("boost_param")
        String boostParam,

        @JsonProperty("prompt")
        String prompt,

        @JsonProperty("keyterms_prompt")
        String keytermsPrompt,

        @JsonProperty("filter_profanity")
        boolean filterProfanity,

        @JsonProperty("redact_pii")
        boolean redactPii,

        @JsonProperty("redact_pii_audio")
        boolean redactPiiAudio,

        @JsonProperty("redact_pii_audio_quality")
        String redactPiiAudioQuality,

        @JsonProperty("redact_pii_audio_options")
        String redactPiiAudioOptions,

        @JsonProperty("redact_pii_policies")
        String redactPiiPolicies,

        @JsonProperty("redact_pii_sub")
        String redactPiiSub,

        @JsonProperty("speaker_labels")
        boolean speakerLabels,

        @JsonProperty("speaker_options")
        String speakerOptions,

        @JsonProperty("content_safety")
        boolean contentSafety,

        @JsonProperty("iab_categories")
        boolean iabCategories,

        @JsonProperty("content_safety_labels")
        Map<String, Object> contentSafetyLabels,

        @JsonProperty("iab_categories_result")
        Map<String, Object> iabCategoriesResult,

        @JsonProperty("language_detection")
        boolean languageDetection,

        @JsonProperty("language_detection_options")
        String languageDetectionOptions,

        @JsonProperty("language_confidence_threshold")
        Double languageConfidenceThreshold,

        @JsonProperty("language_confidence")
        Double languageConfidence,

        @JsonProperty("custom_spelling")
        String customSpelling,

        @JsonProperty("throttled")
        boolean throttled,

        @JsonProperty("auto_chapters")
        boolean autoChapters,

        @JsonProperty("summarization")
        boolean summarization,

        @JsonProperty("summary_type")
        String summaryType,

        @JsonProperty("summary_model")
        String summaryModel,

        @JsonProperty("custom_topics")
        boolean customTopics,

        @JsonProperty("topics")
        List<String> topics,

        @JsonProperty("speech_threshold")
        Double speechThreshold,

        @JsonProperty("speech_model")
        String speechModel,

        @JsonProperty("chapters")
        String chapters,

        @JsonProperty("disfluencies")
        boolean disfluencies,

        @JsonProperty("entity_detection")
        boolean entityDetection,

        @JsonProperty("sentiment_analysis")
        boolean sentimentAnalysis,

        @JsonProperty("sentiment_analysis_results")
        String sentimentAnalysisResults,

        @JsonProperty("entities")
        String entities,

        @JsonProperty("speakers_expected")
        Integer speakersExpected,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("custom_topics_results")
        String customTopicsResults,

        @JsonProperty("is_deleted")
        Boolean isDeleted,

        @JsonProperty("multichannel")
        Boolean multichannel,

        @JsonProperty("project_id")
        Integer projectId,

        @JsonProperty("token_id")
        Integer tokenId
) {
}
