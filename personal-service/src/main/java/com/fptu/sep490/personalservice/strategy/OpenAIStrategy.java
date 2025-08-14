package com.fptu.sep490.personalservice.strategy;

import com.fptu.sep490.personalservice.config.OpenAIProperties;
import com.fptu.sep490.personalservice.repository.client.OpenAIClient;
import com.fptu.sep490.personalservice.viewmodel.request.OpenAIRequest;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import com.fptu.sep490.personalservice.viewmodel.response.OpenAIResponse;
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
public class OpenAIStrategy implements AiApiStrategy {

    // Constants for OpenAI API configuration
    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_CONTEXT = "You are an AI assistant for IELTS Mate system - an IELTS test preparation system for reading and listening skills. Your role is to help suggest topics for learners based on their goals, time set, test results, and other relevant factors. Always provide helpful, educational, and IELTS-focused recommendations.";
    
    // Generation config constants
    private static final double TEMPERATURE = 0.7;
    private static final double TOP_P = 0.8;
    private static final int MAX_TOKENS = 2048;
    private static final int N = 1;
    private static final boolean STREAM = false;

    OpenAIProperties openAIProperties;
    OpenAIClient openAIClient;

    @Override
    public AIResponse callModel(String prompt, String model) {
        try {
            var response = openAIClient.callModel(
                convertToAIBody(prompt, model), 
                "Bearer " + openAIProperties.getKey()
            );
            return UnifiedAIResponse.fromOpenAI(response, model);
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return UnifiedAIResponse.error("Error calling OpenAI API: " + e.getMessage(), "openai");
        }
    }

    private OpenAIRequest convertToAIBody(String prompt, String model) {
        return OpenAIRequest.builder()
                .model(model)
                .messages(createMessages(prompt))
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .topP(TOP_P)
                .n(N)
                .stream(STREAM)
                .build();
    }

    private List<OpenAIRequest.Message> createMessages(String prompt) {
        return List.of(
                createSystemMessage(),
                createUserMessage(prompt)
        );
    }

    private OpenAIRequest.Message createSystemMessage() {
        return OpenAIRequest.Message.builder()
                .role(SYSTEM_ROLE)
                .content(SYSTEM_CONTEXT)
                .build();
    }

    private OpenAIRequest.Message createUserMessage(String prompt) {
        return OpenAIRequest.Message.builder()
                .role(USER_ROLE)
                .content(prompt)
                .build();
    }
}
