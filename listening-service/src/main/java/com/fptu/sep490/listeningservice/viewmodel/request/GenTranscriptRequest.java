package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record GenTranscriptRequest(
        @JsonProperty("audio_url")
        String audioUrl,
        @JsonProperty("language_code")
        String languageCode,
        @JsonProperty("speaker_labels")
        boolean speakerLabels
) {
}
