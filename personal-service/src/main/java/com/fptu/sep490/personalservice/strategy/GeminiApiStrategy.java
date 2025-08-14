package com.fptu.sep490.personalservice.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.personalservice.config.GeminiProperties;
import com.fptu.sep490.personalservice.repository.client.GeminiClient;
import com.fptu.sep490.personalservice.viewmodel.request.GeminiRequest;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import com.fptu.sep490.personalservice.viewmodel.response.GeminiResponse;
import com.fptu.sep490.personalservice.viewmodel.response.UnifiedAIResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GeminiApiStrategy implements AiApiStrategy {

    // API Configuration Constants
    private static final String SYSTEM_ROLE = "user";
    private static final String SYSTEM_CONTEXT = "You are an AI assistant for IELTS Mate system - an IELTS test preparation system for reading and listening skills. Your role is to help suggest topics for learners based on their goals, time set, test results, and other relevant factors. Always provide helpful, educational, and IELTS-focused recommendations.";
    
    // Generation Configuration
    private static final double TEMPERATURE = 0.7;
    private static final double TOP_P = 0.8;
    private static final int TOP_K = 40;
    private static final int MAX_OUTPUT_TOKENS = 2048;
    
    // Safety Settings
    private static final String BLOCK_THRESHOLD = "BLOCK_MEDIUM_AND_ABOVE";
    private static final List<String> SAFETY_CATEGORIES = List.of(
        "HARM_CATEGORY_HARASSMENT",
        "HARM_CATEGORY_HATE_SPEECH", 
        "HARM_CATEGORY_SEXUALLY_EXPLICIT",
        "HARM_CATEGORY_DANGEROUS_CONTENT"
    );

    // Gemini Model Fallback Configuration
    private static final List<String> GEMINI_MODELS = List.of(
        "gemini-2.5-flash",      // Primary model (fastest, most capable)
        "gemini-1.5-pro",        // Fallback 1 (more reliable, slightly slower)
        "gemini-1.5-flash"       // Fallback 2 (legacy, most stable)
    );
    
    private static final int MAX_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000; // Base delay: 1 second
    private static final long MAX_RETRY_DELAY_MS = 10000; // Max delay: 10 seconds

    // Dependencies
    GeminiProperties geminiProperties;
    GeminiClient geminiClient;
    ObjectMapper objectMapper;

    @Override
    public AIResponse callModel(String prompt, String model) {
        try {
            log.info("GeminiApiStrategy: Starting API call for model: {}", model);
            
            // Validate and prepare request
            GeminiRequest requestBody = prepareRequest(prompt);
            
            // Execute API call
            return executeApiCall(requestBody, model);
            
        } catch (Exception e) {
            log.error("GeminiApiStrategy: Error calling Gemini API: {}", e.getMessage(), e);
            return UnifiedAIResponse.error("Error calling Gemini API: " + e.getMessage(), "gemini");
        }
    }


    private GeminiRequest prepareRequest(String prompt) {
        // Input validation
        validatePrompt(prompt);
        
        // Create request body
        GeminiRequest requestBody = buildRequest(prompt);
        
        // Validate and test request
        validateRequest(requestBody);
        
        return requestBody;
    }


    private AIResponse executeApiCall(GeminiRequest requestBody, String model) {
        log.info("GeminiApiStrategy: Starting API call execution");
        
        Exception lastException = null;
        
        // Try each model in order with retries
        for (String geminiModel : GEMINI_MODELS) {
            log.info("GeminiApiStrategy: Attempting to use model: {}", geminiModel);
            
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    log.info("GeminiApiStrategy: Attempt {} for model {}", attempt, geminiModel);
                    
                    AIResponse response = tryModelCall(requestBody, geminiModel);
                    
                    if (response.isSuccess()) {
                        log.info("GeminiApiStrategy: Successfully used model: {} on attempt {}", geminiModel, attempt);
                        return response;
                    } else {
                        log.warn("GeminiApiStrategy: Model {} returned unsuccessful response on attempt {}", geminiModel, attempt);
                        lastException = new RuntimeException("Model returned unsuccessful response");
                    }
                    
                } catch (Exception e) {
                    lastException = e;
                    log.warn("GeminiApiStrategy: Attempt {} failed for model {}: {}", attempt, geminiModel, e.getMessage());
                    
                    // If it's a 503 error, try the next model immediately
                    if (isServiceUnavailableError(e)) {
                        log.info("GeminiApiStrategy: Model {} is overloaded (503), trying next model", geminiModel);
                        break; // Exit retry loop for this model
                    }
                    
                    // For retryable errors, wait and retry
                    if (isRetryableError(e) && attempt < MAX_RETRIES) {
                        long delayMs = calculateRetryDelay(attempt);
                        log.info("GeminiApiStrategy: Retryable error detected, waiting {}ms before retry attempt {}", delayMs, attempt + 1);
                        
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return UnifiedAIResponse.error("Request was interrupted", "gemini");
                        }
                    } else if (!isRetryableError(e)) {
                        // For non-retryable errors, log and try next model
                        log.warn("GeminiApiStrategy: Non-retryable error for model {}, trying next model", geminiModel);
                        break;
                    }
                }
            }
        }
        
        // All models failed
        log.error("GeminiApiStrategy: All models failed after exhausting all retries");
        String errorMessage = lastException != null ? lastException.getMessage() : "All Gemini models are unavailable";
        return UnifiedAIResponse.error("All Gemini models failed: " + errorMessage, "gemini");
    }

    private AIResponse tryModelCall(GeminiRequest requestBody, String geminiModel) {
        log.info("GeminiApiStrategy: Calling Gemini API with model: {} and key: {}...", 
                geminiModel, maskApiKey(geminiProperties.getKey()));
        
        var response = geminiClient.callModel(geminiModel, requestBody, geminiProperties.getKey());
        
        log.info("GeminiApiStrategy: API call successful for model: {}, processing response...", geminiModel);
        
        AIResponse unifiedResponse = UnifiedAIResponse.fromGemini(response.getBody(), geminiModel);
        
        log.info("GeminiApiStrategy: Response converted successfully for model: {}. Success: {}", 
                geminiModel, unifiedResponse.isSuccess());
        
        return unifiedResponse;
    }

    private boolean isServiceUnavailableError(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return false;
        }
        
        // Check for specific 503 and service unavailable errors
        return errorMessage.contains("503") || 
               errorMessage.contains("Service Unavailable") ||
               errorMessage.contains("UNAVAILABLE") ||
               errorMessage.contains("overloaded") ||
               errorMessage.contains("try again later") ||
               errorMessage.contains("rate limit") ||
               errorMessage.contains("quota exceeded") ||
               errorMessage.contains("too many requests");
    }

    private boolean isRetryableError(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return false;
        }
        
        // Check for retryable errors (not 503, but other transient issues)
        return errorMessage.contains("500") ||
               errorMessage.contains("Internal Server Error") ||
               errorMessage.contains("timeout") ||
               errorMessage.contains("connection") ||
               errorMessage.contains("network");
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.error("GeminiApiStrategy: Prompt is null or empty");
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        log.debug("GeminiApiStrategy: Prompt validation passed. Length: {}", prompt.length());
    }


    private GeminiRequest buildRequest(String prompt) {
        return GeminiRequest.builder()
                .contents(createContents(prompt))
                .generationConfig(createGenerationConfig())
                .safetySettings(createSafetySettings())
                .build();
    }


    private void validateRequest(GeminiRequest request) {
        if (request == null || request.getContents() == null || request.getContents().isEmpty()) {
            throw new IllegalStateException("Request body is invalid");
        }
        
        testSerialization(request);
        log.debug("GeminiApiStrategy: Request validation passed");
    }


    private void testSerialization(GeminiRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.debug("GeminiApiStrategy: Serialization test successful. JSON length: {}", json.length());
            
            validateJsonStructure(json);
            
        } catch (Exception e) {
            log.error("GeminiApiStrategy: Serialization test failed: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize request", e);
        }
    }


    private void validateJsonStructure(String json) {
        if (!json.contains("\"text\"") || !json.contains("\"role\"")) {
            log.error("GeminiApiStrategy: Request JSON is missing required fields");
            throw new IllegalStateException("Request structure is invalid");
        }
        
        // Check for unexpected fields
        List<String> unexpectedFields = List.of("audioData", "fileData", "inlineData");
        for (String field : unexpectedFields) {
            if (json.contains(field)) {
                log.error("GeminiApiStrategy: JSON contains unexpected field: {}", field);
                throw new IllegalStateException("Request contains unexpected field: " + field);
            }
        }
    }


    private List<GeminiRequest.Content> createContents(String prompt) {
        return List.of(
                createSystemContent(),
                createUserContent(prompt)
        );
    }


    private GeminiRequest.Content createSystemContent() {
        return GeminiRequest.Content.builder()
                .role(SYSTEM_ROLE)
                .parts(List.of(createTextPart(SYSTEM_CONTEXT)))
                .build();
    }


    private GeminiRequest.Content createUserContent(String prompt) {
        return GeminiRequest.Content.builder()
                .role(SYSTEM_ROLE)
                .parts(List.of(createTextPart(prompt)))
                .build();
    }

    private GeminiRequest.Part createTextPart(String text) {
        return GeminiRequest.Part.builder()
                .text(text)
                .build();
    }


    private GeminiRequest.GenerationConfig createGenerationConfig() {
        return GeminiRequest.GenerationConfig.builder()
                .temperature(TEMPERATURE)
                .topP(TOP_P)
                .topK(TOP_K)
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .build();
    }


    private List<GeminiRequest.SafetySetting> createSafetySettings() {
        return SAFETY_CATEGORIES.stream()
                .map(category -> GeminiRequest.SafetySetting.builder()
                        .category(category)
                        .threshold(BLOCK_THRESHOLD)
                        .build())
                .toList();
    }


    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 10) {
            return "***";
        }
        return apiKey.substring(0, 10) + "...";
    }

    private long calculateRetryDelay(int attempt) {
        if (attempt < 1) {
            return BASE_RETRY_DELAY_MS;
        }
        long delay = BASE_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
}
