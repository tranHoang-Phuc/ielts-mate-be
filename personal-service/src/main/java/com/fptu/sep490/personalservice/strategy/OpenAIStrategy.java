package com.fptu.sep490.personalservice.strategy;

import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenAIStrategy implements AiApiStrategy{
    @Override
    public AIResponse callModel(String prompt, String model) {
        return null;
    }
}
