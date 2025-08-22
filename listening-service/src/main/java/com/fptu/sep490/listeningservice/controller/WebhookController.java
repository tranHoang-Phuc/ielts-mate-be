package com.fptu.sep490.listeningservice.controller;

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
    
    @PostMapping("/assembly-ai/transcript")
    public ResponseEntity<Void> handleTranscriptWebhook(
            @RequestBody AssemblyAIWebhookRequest webhookRequest,
            @RequestHeader(value = "X-AssemblyAI-Signature", required = false) String signature) {
        
        try {
            log.info("Received AssemblyAI webhook for transcript ID: {}", webhookRequest.transcriptId());
            
            // Validate webhook signature if needed
            // transcriptWebhookService.validateSignature(webhookRequest, signature);
            
            transcriptWebhookService.processTranscriptWebhook(webhookRequest);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing AssemblyAI webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
