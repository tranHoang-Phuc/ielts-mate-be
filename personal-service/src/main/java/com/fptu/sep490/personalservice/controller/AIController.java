package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
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
    public ResponseEntity<BaseResponse<AIResponse>> chatWithGemini(HttpServletRequest request) {
        AIResponse data = aiService.callAIForSuggesting(request);
        BaseResponse<AIResponse> response = BaseResponse.<AIResponse>builder()
                .data(data)
                .message("Receive suggestion successfully")
                .build();
        return ResponseEntity.ok(response);
    }



}
