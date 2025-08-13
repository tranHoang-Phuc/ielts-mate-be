package com.fptu.sep490.personalservice.strategy;

import com.fptu.sep490.commonlibrary.constants.AIModel;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIStrategyFactory {

    private final Map<String, AiApiStrategy> strategyMap;

    @Autowired
    public AIStrategyFactory(List<AiApiStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(this::getStrategyKey, Function.identity()));
    }

    public AiApiStrategy getStrategy(String model) {
        String strategyKey = getStrategyKeyFromModel(model);
        AiApiStrategy strategy = strategyMap.get(strategyKey);
        
        if (strategy == null) {
            log.warn("No strategy found for model: {}. Using default Gemini strategy.", model);
            strategy = strategyMap.get("gemini");
        }
        
        return strategy;
    }

    private String getStrategyKey(AiApiStrategy strategy) {
        if (strategy instanceof GeminiApiStrategy) {
            return "gemini";
        } else if (strategy instanceof OpenAIStrategy) {
            return "openai";
        }
        return "unknown";
    }

    private String getStrategyKeyFromModel(String model) {
        if (model == null) {
            return "gemini"; // default
        }
        
        if (model.contains("Flash") || model.contains("Gemini") || model.contains("2.5")) {
            return "gemini";
        } else if (model.contains("OpenAI") || model.contains("gpt")) {
            return "openai";
        }
        
        return "gemini"; // default fallback
    }

    public boolean isModelSupported(String model) {
        String strategyKey = getStrategyKeyFromModel(model);
        return strategyMap.containsKey(strategyKey);
    }

    public List<String> getSupportedModels() {
        return strategyMap.keySet().stream()
                .map(key -> {
                    switch (key) {
                        case "gemini":
                            return AIModel.Gemini.FLASH2_5;
                        case "openai":
                            return AIModel.OpenAI.OPEN3_5;
                        default:
                            return "Unknown";
                    }
                })
                .toList();
    }
}
