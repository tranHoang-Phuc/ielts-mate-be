package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GeminiResponse extends AIResponse{
    private List<Candidate> candidates;
    private UsageMetadata usageMetadata;
    private String modelVersion;

    @Data
    @Builder
    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
        private List<SafetyRating> safetyRatings;
        private CitationMetadata citationMetadata;
    }

    @Data
    @Builder
    public static class Content {
        private List<Part> parts;
        private String role; // "user" hoặc "model"
    }

    @Data
    @Builder
    public static class Part {
        private String text;
        private InlineData inlineData;
        private FileData fileData;
        private AudioData audioData;
    }

    @Data
    @Builder
    public static class InlineData {
        private String mimeType;
        private String data;
    }

    @Data
    @Builder
    public static class FileData {
        private String mimeType;
        private String fileUri;
    }

    @Data
    @Builder
    public static class AudioData {
        private String mimeType;
        private String data;
    }

    @Data
    @Builder
    public static class SafetyRating {
        private String category;
        private String probability;
    }

    @Data
    @Builder
    public static class UsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;
    }

    @Data
    @Builder
    public static class CitationMetadata {
        private List<Citation> citations;
    }

    @Data
    @Builder
    public static class Citation {
        private String startIndex;
        private String endIndex;
        private String uri;
        private String title;
    }
}