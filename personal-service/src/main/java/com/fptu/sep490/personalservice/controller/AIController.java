package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import com.fptu.sep490.personalservice.viewmodel.response.AISuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "AI Controller", description = "Endpoints for AI model interactions")
public class AIController {

    AIService aiService;

    @GetMapping("/gemini/suggest")
    @Operation(
        summary = "Chat with Gemini AI",
        description = "Send a prompt specifically to Gemini AI model and get a response"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Gemini AI response received successfully",
            content = @Content(schema = @Schema(implementation = AIResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or Gemini service error"
        )
    })
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<BaseResponse<AIResponse>> chatWithGemini(HttpServletRequest request) {
        AIResponse data = aiService.callAIForSuggesting(request);
        BaseResponse<AIResponse> response = BaseResponse.<AIResponse>builder()
                .data(data)
                .message("Receive suggestion successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestion")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<BaseResponse<AISuggestionResponse>> getCurrentSuggestion(HttpServletRequest request) {
        AISuggestionResponse data = aiService.getCurrentSuggestion(request);
        BaseResponse<AISuggestionResponse> response = BaseResponse.<AISuggestionResponse>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ielts/send")
    @Operation(summary = "Send a message in IELTS Chat", description = "Send a message and get AI response for IELTS learning")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<AIResponse>> sendIeltsMessage(
            @RequestBody String message
    ) {
        AIResponse data = aiService.chat(message, null);
        BaseResponse<AIResponse> response = BaseResponse.<AIResponse>builder()
                .data(data)
                .message("AI response received successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ielts/history")
    @Operation(summary = "Get IELTS Chat history", description = "Retrieve the current chat history for a given session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<AIService.ChatMessage>>> getIeltsHistory(
    ) {
        List<AIService.ChatMessage> history = aiService.getHistory();
        BaseResponse<List<AIService.ChatMessage>> response = BaseResponse.<List<AIService.ChatMessage>>builder()
                .data(history)
                .message("Chat history retrieved successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/ielts/clear")
    @Operation(summary = "Clear IELTS Chat history", description = "Clear the chat history for a given session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> clearIeltsHistory(
    ) {
        aiService.clearSession();
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .message("Chat history cleared successfully")
                .build();
        return ResponseEntity.ok(response);
    }




}
