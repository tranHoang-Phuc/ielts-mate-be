package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Data;
import java.util.List;

@Data
public class GeminiResponse extends AIResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;

    @Override
    public String getContent() {
        if (candidates != null && !candidates.isEmpty() && candidates.get(0).content != null) {
            List<Part> parts = candidates.get(0).content.parts;
            if (parts != null && !parts.isEmpty()) {
                return parts.get(0).text;
            }
        }
        return "";
    }

    @Override
    public boolean isSuccess() {
        return candidates != null && !candidates.isEmpty() && 
               candidates.get(0).content != null && 
               candidates.get(0).finishReason.equals("STOP");
    }

    @Data
    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
        private List<SafetyRating> safetyRatings;
    }

    @Data
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    public static class Part {
        private String text;
        private InlineData inlineData;
    }

    @Data
    public static class InlineData {
        private String mimeType;
        private String data;
    }

    @Data
    public static class SafetyRating {
        private String category;
        private String probability;
    }

    @Data
    public static class PromptFeedback {
        private List<SafetyRating> safetyRatings;
    }
}