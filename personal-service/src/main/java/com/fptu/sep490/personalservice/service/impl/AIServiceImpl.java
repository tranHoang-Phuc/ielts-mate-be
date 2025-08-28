package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.constants.AIModel;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.AISuggestion;
import com.fptu.sep490.personalservice.model.TopicMaster;
import com.fptu.sep490.personalservice.model.enumeration.LangGuage;
import com.fptu.sep490.personalservice.repository.AISuggestionRepository;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.TopicMaterRepository;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.strategy.AIStrategyFactory;
import com.fptu.sep490.personalservice.strategy.AiApiStrategy;
import com.fptu.sep490.personalservice.strategy.GeminiApiStrategy;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import com.fptu.sep490.personalservice.viewmodel.response.AIResultData;
import com.fptu.sep490.personalservice.viewmodel.response.AISuggestionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final GeminiApiStrategy geminiApiStrategy;
    HttpSession httpSession;
    AISuggestionRepository aiSuggestionRepository;

    private final Map<String, List<ChatMessage>> chatHistories = new ConcurrentHashMap<>();

    private static final String SYSTEM_CONTEXT =
            "You are an AI IELTS tutor. Always respond in a helpful, educational, and IELTS-focused way. " +
                    "If the user asks for more details, provide a longer and more comprehensive answer as needed."+
                    "If the user does not explicitly request a long answer, respond concisely in 3-4 sentences suitable for chat messages. " ;

    private String getSessionId() {
        return httpSession.getId();
    }
    public AIResponse callAIForSuggesting(HttpServletRequest request) {
        try {
            String accessToken = helper.getAccessToken(request);
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
            var readingDataRes = readingClient.getAIData("Bearer " + accessToken);
            var listeningDataRes = listeningClient.getAIData("Bearer " + accessToken);
            if (readingDataRes.getStatusCode() != HttpStatus.OK || listeningDataRes.getStatusCode() != HttpStatus.OK ||
                readingDataRes.getBody() == null || listeningDataRes.getBody() == null) {
                throw new AppException(
                        Constants.ErrorCodeMessage.FAILED_TO_GET_PRACTICE_DATA,
                        Constants.ErrorCode.FAILED_TO_GET_PRACTICE_DATA,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
            var readingBody = readingDataRes.getBody();
            var listeningBody = listeningDataRes.getBody();
            List<AIResultData> readingData = readingBody != null ? readingBody.data() : new ArrayList<>();
            List<AIResultData> listeningData = listeningBody != null ? listeningBody.data() : new ArrayList<>();
            String practiceResult = objectMapper.writeValueAsString(Map.of(
                    "reading", readingData,
                    "listening", listeningData
            ));
            log.info("Retrieved practice results for user {}: {}", userId, practiceResult);

            // Create a structured prompt for better AI response
            String prompt = createStructuredPrompt(userTargetConfig, objectMapper.writeValueAsString(topicMasters), practiceResult);
            log.info("Generated prompt for user {}: {}", userId, prompt);
            
            // Get AI strategy and call model
            AiApiStrategy strategy = aiStrategyFactory.getStrategy(AIModel.Gemini.FLASH2_5);
            log.info("Using AI strategy: {}", strategy.getClass().getSimpleName());
            
            AIResponse response = strategy.callModel(prompt, AIModel.Gemini.FLASH2_5);
            log.info("AI response received successfully for user: {}", userId);

            var currentSuggestion = aiSuggestionRepository.findByCreatedBy(userId)
                    .orElse(null);

            if (currentSuggestion == null) {
                AISuggestion suggestion = AISuggestion.builder()
                        .suggestionData(response.getContent())
                        .createdBy(userId)
                        .build();
                aiSuggestionRepository.save(suggestion);

            } else {
                currentSuggestion.setSuggestionData(response.getContent());
                aiSuggestionRepository.save(currentSuggestion);
            }
            return response;
            
        } catch (AppException e) {
            log.error("AppException in callAIForSuggesting: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in callAIForSuggesting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call AI model: " + e.getMessage(), e);
        }
    }

    @Override
    public AIResponse chat( String userMessage, String sessionId) {
        sessionId = sessionId == null ? getSessionId() : sessionId;
        List<ChatMessage> history = chatHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // Nếu là tin nhắn đầu tiên, thêm system context
        if (history.isEmpty()) {
            history.add(new ChatMessage("system", SYSTEM_CONTEXT));
        }

        // Thêm tin nhắn của user
        history.add(new ChatMessage("user", userMessage));

        // Gộp lịch sử thành 1 prompt
        String prompt = buildPromptFromHistory(history);

        // Gọi Gemini API
        AIResponse response = geminiApiStrategy.callModel(prompt, null);

        if (response.isSuccess()) {
            history.add(new ChatMessage("assistant", response.getContent()));
        }

        return response;
    }

    @Override
    public void clearSession() {
        chatHistories.remove(getSessionId());
    }

    @Override
    public List<ChatMessage> getHistory() {
        return chatHistories.getOrDefault(getSessionId(), List.of());
    }

    @Override
    public AISuggestionResponse getCurrentSuggestion(HttpServletRequest request) {
        AISuggestion currentSuggestion = aiSuggestionRepository.findByCreatedBy(helper.getUserIdFromToken())
                .orElse(null);
        if (currentSuggestion == null) {
            return AISuggestionResponse.builder().suggestion(null).build();
        } else {
            return AISuggestionResponse.builder().suggestion(currentSuggestion.getSuggestionData()).build();
        }
    }


    @Override
    public void clearSession(String sessionId) {
        chatHistories.remove(sessionId);

    }

    private String buildPromptFromHistory(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString().trim();
    }

    private String createStructuredPrompt(String targetConfig, String systemTopic, String practiceResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an experienced IELTS tutor specializing in personalized learning plans.\n");
        prompt.append("Your task is to analyze the user's information and create customized study task suggestions.\n\n");

        prompt.append("=== USER INFORMATION ===\n");
        prompt.append("Target Configuration:\n");
        prompt.append(targetConfig).append("\n");
        prompt.append("System Topic (focus areas or current lesson theme):\n");
        prompt.append(systemTopic).append("\n");
        prompt.append("Practice Results:\n");
        prompt.append(practiceResult).append("\n\n");

        prompt.append("=== OUTPUT REQUIREMENTS ===\n");
        prompt.append("Analyze the user's practice results, strengths, and weaknesses, then provide EXACTLY 3-4 specific study tasks.\n");
        prompt.append("Focus on areas where the user needs the most improvement based on their recent performance.\n");
        prompt.append("Cover at least 2 of these 3 skills ONLY: Listening, Reading, Vocabulary.\n\n");

        prompt.append("Return ONLY a valid JSON array in this exact format:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"title\": \"Take a Listening Mock Test\",\n");
        prompt.append("    \"description\": \"You haven't taken a full listening exam in 2 weeks. Practice with a complete mock test to maintain timing skills.\",\n");
        prompt.append("    \"priority\": 0,\n");
        prompt.append("    \"duration\": \"45 min\",\n");
        prompt.append("    \"skill\": \"Listening\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");

        prompt.append("IMPORTANT GUIDELINES:\n");
        prompt.append("- priority: 0 (highest) to 3 (lowest) based on user's weakness level\n");
        prompt.append("- duration: realistic time estimates (e.g., \"30 min\", \"45 min\", \"60 min\")\n");
        prompt.append("- skill: Must be EXACTLY one of \"Listening\", \"Reading\", \"Vocabulary\" ONLY\n");
        prompt.append("- title: Clear, actionable task names\n");
        prompt.append("- description: Specific, personalized explanations based on user's data\n");
        prompt.append("- Return valid JSON only, no additional text or markdown formatting\n");

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

    public String generateSemanticContextFromMeaning(String word, String meaning) {
        try {
            String prompt = createContextGenerationPrompt(word, meaning);
            log.info("Generated context prompt for word '{}': {}", word, prompt);

            AiApiStrategy strategy = aiStrategyFactory.getStrategy(AIModel.Gemini.FLASH2_5);
            AIResponse aiResponse = strategy.callModel(prompt, AIModel.Gemini.FLASH2_5);

            if (aiResponse != null && aiResponse.getContent() != null && !aiResponse.getContent().isBlank()) {
                return aiResponse.getContent().trim().toLowerCase();
            } else {
                throw new AppException(
                        "AI_MODEL_EMPTY_CONTEXT",
                        "AI model returned empty response for semantic context generation",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
        } catch (Exception e) {
            log.error("Error generating semantic context for word '{}' with meaning '{}': {}", word, meaning, e.getMessage(), e);
            throw new RuntimeException("Failed to generate semantic context: " + e.getMessage(), e);
        }
    }

    private String createContextGenerationPrompt(String word, String meaning) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a linguistic categorizer.\n");
        prompt.append("You are an advanced English-English dictionary.\n");
        prompt.append("Based on the word and its meaning, return ONLY one short context/domain.\n");
        prompt.append("Rules:\n");
        prompt.append("- Do not explain.\n");
        prompt.append("- Do not output multiple options.\n\n");

        prompt.append("=== WORD ===\n").append(word).append("\n");
        prompt.append("=== MEANING ===\n").append(meaning).append("\n");

        return prompt.toString();
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
            prompt.append("Only return the most common Vietnamese meaning of the word. ");
            prompt.append("If the context is irrelevant, ignore it and return the primary meaning. ");
            prompt.append("Do not list multiple meanings. No extra explanation, no English.\n");
        } else if (language == LangGuage.ENGLISH) {
            prompt.append("Only return the most common English meaning of the word. ");
            prompt.append("If the context is irrelevant, ignore it and return the primary meaning. ");
            prompt.append("Do not list multiple meanings. No translation, no extra explanation.\n");
        }

        return prompt.toString();
    }

}
