package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AIService {
     AIResponse callAIForSuggesting(HttpServletRequest request);
}
