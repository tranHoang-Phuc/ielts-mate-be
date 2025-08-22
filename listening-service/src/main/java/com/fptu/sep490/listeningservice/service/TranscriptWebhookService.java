package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.AssemblyAIWebhookRequest;

public interface TranscriptWebhookService {
    void processTranscriptWebhook(AssemblyAIWebhookRequest webhookRequest);
    void validateSignature(AssemblyAIWebhookRequest request, String signature);
}
