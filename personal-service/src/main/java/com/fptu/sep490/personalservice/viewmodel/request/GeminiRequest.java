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
        private String role; // "user" hoặc "model"
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class Part {
        private String text; // Văn bản
        private InlineData inlineData; // Dữ liệu base64 (ảnh, pdf, audio...)
        private FileData fileData; // File URI từ Google Cloud
        private AudioData audioData; // Audio gửi lên (base64 + mimeType)
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
    public static class GenerationConfig {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxOutputTokens;
        private String responseMimeType;
    }

    @Data
    @Builder
    public static class SafetySetting {
        private String category;
        private String threshold;
    }
}
