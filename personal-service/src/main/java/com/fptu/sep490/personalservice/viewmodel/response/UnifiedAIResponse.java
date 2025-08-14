package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnifiedAIResponse extends AIResponse {
    private String content;
    private boolean success;
    private String provider; // "gemini" or "openai"
    private String model;
    private String errorMessage;

    public UnifiedAIResponse(String content, boolean success, String provider, String model) {
        this.content = content;
        this.success = success;
        this.provider = provider;
        this.model = model;
    }

    public UnifiedAIResponse(String errorMessage, String provider) {
        this.content = "";
        this.success = false;
        this.provider = provider;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    public static UnifiedAIResponse fromGemini(GeminiResponse geminiResponse, String model) {
        return new UnifiedAIResponse(
            geminiResponse.getContent(),
            geminiResponse.isSuccess(),
            "gemini",
            model
        );
    }

    public static UnifiedAIResponse fromOpenAI(OpenAIResponse openAIResponse, String model) {
        String content = "";
        boolean success = false;
        
        if (openAIResponse.getChoices() != null && !openAIResponse.getChoices().isEmpty()) {
            content = openAIResponse.getChoices().get(0).getMessage().getContent();
            success = "stop".equals(openAIResponse.getChoices().get(0).getFinishReason());
        }
        
        return new UnifiedAIResponse(content, success, "openai", model);
    }

    public static UnifiedAIResponse error(String errorMessage, String provider) {
        return new UnifiedAIResponse(errorMessage, provider);
    }
}
