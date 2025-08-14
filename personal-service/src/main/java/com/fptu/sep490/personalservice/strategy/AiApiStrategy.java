package com.fptu.sep490.personalservice.strategy;

import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;

public interface AiApiStrategy {
    AIResponse callModel(String prompt, String model);
}
