package com.fptu.sep490.personalservice.strategy;

import com.fptu.sep490.personalservice.config.GeminiProperties;
import com.fptu.sep490.personalservice.repository.client.GeminiClient;
import com.fptu.sep490.personalservice.viewmodel.request.GeminiRequest;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GeminiApiStrategy implements AiApiStrategy{

    GeminiProperties geminiProperties;
    GeminiClient geminiClient;

    @Override
    public AIResponse callModel(String prompt, String model) {
        return null;
    }

    private GeminiRequest convertToAIBody(String prompt) {
        return null;
    }
}
