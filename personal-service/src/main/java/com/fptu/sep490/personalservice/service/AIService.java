package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AIService {
     AIResponse callAIForSuggesting(HttpServletRequest request);

     AIResponse chat( String userMessage,String sessionId);
     void clearSession();
     List<ChatMessage> getHistory();

     record ChatMessage(String role, String content) {}
     void clearSession(String sessionId);
}
