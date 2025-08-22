package com.fptu.sep490.listeningservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.listeningservice.service.TranscriptWebhookService;
import com.fptu.sep490.listeningservice.viewmodel.request.AssemblyAIWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final TranscriptWebhookService transcriptWebhookService;
    private final ObjectMapper objectMapper;
    
    @PostMapping("/assembly-ai/transcript")
    public ResponseEntity<Void> handleTranscriptWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-AssemblyAI-Signature", required = false) String signature) {
        
        try {
            log.info("Received AssemblyAI webhook");
            log.debug("Raw payload: {}", rawPayload);
            log.debug("Signature: {}", signature);
            
            // Parse the raw payload to understand the structure
            JsonNode jsonNode = objectMapper.readTree(rawPayload);
            
            // Log the structure for debugging
            log.info("Webhook JSON structure: {}", jsonNode.toPrettyString());
            
            // Try to parse as AssemblyAIWebhookRequest
            AssemblyAIWebhookRequest webhookRequest = objectMapper.treeToValue(jsonNode, AssemblyAIWebhookRequest.class);
            
            log.info("Parsed webhook - ID: {}, Status: {}, Text present: {}", 
                    webhookRequest.getTranscriptId(), 
                    webhookRequest.status(), 
                    webhookRequest.text() != null && !webhookRequest.text().isEmpty());
            
            if (webhookRequest.text() != null) {
                log.info("Text preview: {}", webhookRequest.text().substring(0, Math.min(webhookRequest.text().length(), 100)) + "...");
            }
            
            // Process the webhook
            transcriptWebhookService.processTranscriptWebhook(webhookRequest);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing AssemblyAI webhook: ", e);
            log.error("Raw payload that caused error: {}", rawPayload);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Debug endpoint to test webhook parsing
    @PostMapping("/assembly-ai/test")
    public ResponseEntity<String> testWebhookParsing(@RequestBody String rawPayload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawPayload);
            AssemblyAIWebhookRequest webhookRequest = objectMapper.treeToValue(jsonNode, AssemblyAIWebhookRequest.class);
            
            return ResponseEntity.ok(String.format(
                "Parsed successfully - ID: %s, Status: %s, Text: %s", 
                webhookRequest.getTranscriptId(), 
                webhookRequest.status(), 
                webhookRequest.text() != null ? "Present (" + webhookRequest.text().length() + " chars)" : "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Parsing error: " + e.getMessage());
        }
    }
}
