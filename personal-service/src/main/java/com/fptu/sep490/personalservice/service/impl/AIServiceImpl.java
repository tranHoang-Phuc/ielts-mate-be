package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.constants.AIModel;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.TopicMaster;
import com.fptu.sep490.personalservice.model.enumeration.LangGuage;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.TopicMaterRepository;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.strategy.AIStrategyFactory;
import com.fptu.sep490.personalservice.strategy.AiApiStrategy;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.internals.Topic;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIServiceImpl implements AIService {

    AIStrategyFactory aiStrategyFactory;
    ConfigRepository configRepository;
    TopicMaterRepository topicMaterRepository;
    Helper helper;
    ObjectMapper objectMapper;
    ReadingClient readingClient;
    ListeningClient listeningClient;

    public AIResponse callAIForSuggesting(HttpServletRequest request) {
        try {
            String userId = helper.getUserIdFromToken();
            log.info("Processing AI suggestion request for user: {}", userId);
            
            // Get user's target configuration
            var userTargetConfig = configRepository.getConfigByKeyAndAccountId(
                    Constants.Config.TARGET_CONFIG, 
                    UUID.fromString(userId)
            ).orElseThrow(() -> new AppException(
                    Constants.ErrorCodeMessage.NEED_TO_CONFIG_TARGET,
                    Constants.ErrorCode.NEED_TO_CONFIG_TARGET, 
                    HttpStatus.BAD_REQUEST.value()
            ));
            
            log.info("Retrieved target config for user {}: {}", userId, userTargetConfig);

            // Get system topic
            List<TopicMaster> topicMasters = topicMaterRepository.findAll();

            // Call to set user test perform data


            // Create a structured prompt for better AI response
            String prompt = createStructuredPrompt(userTargetConfig, objectMapper.writeValueAsString(topicMasters),"");
            log.info("Generated prompt for user {}: {}", userId, prompt);
            
            // Get AI strategy and call model
            AiApiStrategy strategy = aiStrategyFactory.getStrategy(AIModel.Gemini.FLASH2_5);
            log.info("Using AI strategy: {}", strategy.getClass().getSimpleName());
            
            AIResponse response = strategy.callModel(prompt, AIModel.Gemini.FLASH2_5);
            log.info("AI response received successfully for user: {}", userId);
            
            return response;
            
        } catch (AppException e) {
            log.error("AppException in callAIForSuggesting: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in callAIForSuggesting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call AI model: " + e.getMessage(), e);
        }
    }

    private String createStructuredPrompt(String targetConfig, String systemTopic, String practiceResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an experienced IELTS tutor specializing in personalized learning plans.\n");
        prompt.append("Your task is to analyze the user's information and create a customized study suggestion.\n\n");

        prompt.append("=== USER INFORMATION ===\n");
        prompt.append("Target Configuration:\n");
        prompt.append(targetConfig).append("\n");
        prompt.append("System Topic (focus areas or current lesson theme):\n");
        prompt.append(systemTopic).append("\n");
        prompt.append("Practice Results:\n");
        prompt.append(practiceResult).append("\n\n");

        prompt.append("=== OUTPUT REQUIREMENTS ===\n");
        prompt.append("Provide a detailed IELTS study suggestion containing:\n");
        prompt.append("1. **3-5 specific IELTS topics** the user should focus on (tailored to weaknesses & goals).\n");
        prompt.append("2. **Recommended study schedule** for the next 7 days (daily breakdown).\n");
        prompt.append("3. **Specific skills** to improve (e.g., listening for detail, reading skimming).\n");
        prompt.append("4. **Practice exercises or resources** (include type and source).\n");
        prompt.append("5. **Motivation tips** (practical, encouraging, and realistic).\n\n");

        prompt.append("Make the suggestions practical, achievable, and aligned with the user's current level.\n");
        prompt.append("Use clear section headings and bullet points for readability.\n");

        return prompt.toString();
    }
    /**
     * Gọi AI để định nghĩa một từ vựng kèm context.
     *
     * @param word     Từ cần định nghĩa
     * @param context  Ngữ cảnh (có thể null hoặc rỗng)
     * @param language Ngôn ngữ muốn nhận kết quả (ENGLISH hoặc VIETNAMESE)
     * @return Nghĩa của từ theo ngôn ngữ yêu cầu
     */
    public String getVocabularyDefinition(String word, String context, LangGuage language) {
        try {
            // Tạo prompt dựa trên ngôn ngữ
            String prompt = createVocabularyDefinitionPrompt(word, context, language);
            log.info("Generated vocabulary prompt for word '{}' with language {}: {}", word, language, prompt);

            // Chọn AI strategy (ví dụ Gemini)
            AiApiStrategy strategy = aiStrategyFactory.getStrategy(AIModel.Gemini.FLASH2_5);
            log.info("Using AI strategy: {}", strategy.getClass().getSimpleName());

            // Gọi AI model
            AIResponse aiResponse = strategy.callModel(prompt, AIModel.Gemini.FLASH2_5);

            // Lấy nội dung nghĩa
            if (aiResponse != null && aiResponse.getContent() != null && !aiResponse.getContent().isBlank()) {
                return aiResponse.getContent().trim();
            } else {
                throw new AppException(
                        "AI_MODEL_EMPTY_RESPONSE",
                        "AI model returned empty response for vocabulary definition",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
        } catch (Exception e) {
            log.error("Error getting vocabulary definition for word '{}': {}", word, e.getMessage(), e);
            throw new RuntimeException("Failed to get vocabulary definition: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo prompt cho AI để định nghĩa từ vựng theo ngôn ngữ yêu cầu
     */
    private String createVocabularyDefinitionPrompt(String word, String context, LangGuage language) {
        StringBuilder prompt = new StringBuilder();

        // Chọn hướng dẫn theo ngôn ngữ
        if (language == LangGuage.VIETNAMESE) {
            prompt.append("You are an advanced English-Vietnamese dictionary.\n");
            prompt.append("Your task is to provide a short, clear, and accurate definition in Vietnamese.\n\n");
        } else {
            prompt.append("You are an advanced English-English dictionary.\n");
            prompt.append("Your task is to provide a short, clear, and accurate definition in English.\n\n");
        }

        prompt.append("=== WORD ===\n").append(word).append("\n");
        if (context != null && !context.isBlank()) {
            prompt.append("=== CONTEXT ===\n").append(context).append("\n");
        }

        prompt.append("\n=== OUTPUT REQUIREMENTS ===\n");
        if (language == LangGuage.VIETNAMESE) {
            prompt.append("Only return the Vietnamese meaning of the word. No extra explanation, no English.\n");
        } else if (language == LangGuage.ENGLISH) {
            prompt.append("Only return the English meaning of the word. No translation, no extra explanation.\n");
        }

        return prompt.toString();
    }

}
