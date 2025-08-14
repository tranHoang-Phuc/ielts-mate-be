package com.fptu.sep490.personalservice.viewmodel.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GeminiRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;
    private List<SafetySetting> safetySettings;

    @Data
    @Builder
    public static class Content {
        private String role; // "user" or "model"
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class Part {
        private String text; // Text content only
    }

    @Data
    @Builder
    public static class GenerationConfig {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxOutputTokens;
    }

    @Data
    @Builder
    public static class SafetySetting {
        private String category;
        private String threshold;
    }
}
