package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.repository.TranscriptStatusRepository;
import com.fptu.sep490.listeningservice.repository.client.AssemblyAIClient;
import com.fptu.sep490.listeningservice.service.TranscriptWebhookService;
import com.fptu.sep490.listeningservice.viewmodel.request.AssemblyAIWebhookRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.TranscriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
public class TranscriptWebhookServiceImpl implements TranscriptWebhookService {
    
    ListeningTaskRepository listeningTaskRepository;
    TranscriptStatusRepository transcriptStatusRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    RedisTemplate<String, Object> redisTemplate;
    AssemblyAIClient assemblyAIClient;
    
    @Value("${topic.send-notification}")
    @NonFinal
    String sendNotificationTopic;
    
    @Value("${assembly-ai.api-key}")
    @NonFinal
    String assemblyAIApiKey;
    
    private static final String TRANSCRIPT_CACHE_PREFIX = "transcript:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    @Override
    @Transactional
    public void processTranscriptWebhook(AssemblyAIWebhookRequest webhookRequest) {
        UUID transcriptId = webhookRequest.getTranscriptId(); // Use the helper method
        String status = webhookRequest.status();
        
        log.info("Processing webhook for transcript ID: {} with status: {}", transcriptId, status);
        
        // Update transcript status in database
        TranscriptStatus transcriptStatus = transcriptStatusRepository
                .findByTranscriptId(transcriptId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        
        transcriptStatus.setStatus(status);
        transcriptStatus.setTranscriptText(webhookRequest.text());
        log.info("Transcript text {}",webhookRequest.text());
        transcriptStatus.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        
        if ("completed".equalsIgnoreCase(status)) {
            handleCompletedTranscript(webhookRequest, transcriptStatus);
        } else if ("error".equalsIgnoreCase(status)) {
            handleErrorTranscript(webhookRequest, transcriptStatus);
        }
        
        transcriptStatusRepository.save(transcriptStatus);
        
        // Update cache
        updateTranscriptCache(transcriptId, webhookRequest);
    }
    
    private void handleCompletedTranscript(AssemblyAIWebhookRequest webhookRequest, TranscriptStatus transcriptStatus) {
        UUID taskId = transcriptStatus.getTaskId();
        UUID transcriptId = webhookRequest.getTranscriptId();
        
        try {
            log.info("Fetching complete transcript data from AssemblyAI for transcript ID: {}", transcriptId);
            
            // Use OpenFeign client to get the complete transcript
            var response = assemblyAIClient.getTranscript(transcriptId, assemblyAIApiKey);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TranscriptResponse transcriptData = response.getBody();
                
                log.info("Successfully retrieved transcript data from AssemblyAI. Status: {}, Text length: {}", 
                        transcriptData.status(), 
                        transcriptData.text() != null ? transcriptData.text().length() : 0);
                
                // Use the data from AssemblyAI API response
                String transcriptText = transcriptData.text();
                Double confidence = transcriptData.confidence();
                Double audioDuration = transcriptData.audioDuration();
                
                // Update transcript status with complete data
                transcriptStatus.setTranscriptText(transcriptText);
                transcriptStatus.setConfidence(confidence);
                transcriptStatus.setAudioDuration(audioDuration);
                
                // Update all versions of the listening task
                List<ListeningTask> allVersions = listeningTaskRepository.findAllVersion(taskId);
                for (ListeningTask listeningTask : allVersions) {
                    listeningTask.setTranscription(transcriptText);
                }
                listeningTaskRepository.saveAll(allVersions);
                
                // Send success notification
                sendNotification(transcriptStatus.getCreatedBy(), "success", 
                        "Your audio transcription has been completed successfully.");
                
                log.info("Successfully processed completed transcript for task ID: {} with {} characters", 
                        taskId, transcriptText != null ? transcriptText.length() : 0);
                
            } else {
                log.error("Failed to fetch transcript from AssemblyAI. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                
                // Fallback to webhook data if API call fails
                handleCompletedTranscriptFallback(webhookRequest, transcriptStatus, taskId);
            }
            
        } catch (Exception e) {
            log.error("Error fetching transcript from AssemblyAI for transcript ID: {}", transcriptId, e);
            
            // Fallback to webhook data if API call fails
            handleCompletedTranscriptFallback(webhookRequest, transcriptStatus, taskId);
        }
    }
    
    private void handleCompletedTranscriptFallback(AssemblyAIWebhookRequest webhookRequest, 
                                                   TranscriptStatus transcriptStatus, 
                                                   UUID taskId) {
        log.warn("Using fallback method with webhook data for transcript ID: {}", webhookRequest.getTranscriptId());
        
        String transcriptText = webhookRequest.text();
        
        // Update transcript status with webhook data
        transcriptStatus.setTranscriptText(transcriptText);
        transcriptStatus.setConfidence(webhookRequest.confidence());
        transcriptStatus.setAudioDuration(webhookRequest.audioDuration());
        
        // Update all versions of the listening task
        List<ListeningTask> allVersions = listeningTaskRepository.findAllVersion(taskId);
        for (ListeningTask listeningTask : allVersions) {
            listeningTask.setTranscription(transcriptText);
        }
        listeningTaskRepository.saveAll(allVersions);
        
        // Send success notification
        sendNotification(transcriptStatus.getCreatedBy(), "success", 
                "Your audio transcription has been completed successfully.");
        
        log.info("Successfully processed completed transcript using fallback for task ID: {}", taskId);
    }
    
    private void handleErrorTranscript(AssemblyAIWebhookRequest webhookRequest, TranscriptStatus transcriptStatus) {
        transcriptStatus.setErrorMessage(webhookRequest.error());
        
        // Send error notification
        sendNotification(transcriptStatus.getCreatedBy(), "error", 
                "There was an error processing your audio transcription: " + webhookRequest.error());
        
        log.error("Transcript processing failed for ID: {} with error: {}", 
                webhookRequest.getTranscriptId(), webhookRequest.error());
    }
    
    private void updateTranscriptCache(UUID transcriptId, AssemblyAIWebhookRequest webhookRequest) {
        String cacheKey = TRANSCRIPT_CACHE_PREFIX + transcriptId;
        try {
            redisTemplate.opsForValue().set(cacheKey, webhookRequest, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to update transcript cache for ID: {}", transcriptId, e);
        }
    }
    
    private void sendNotification(String userId, String status, String message) {
        try {
            SseEvent sseEvent = SseEvent.builder()
                    .clientId(UUID.fromString(userId))
                    .status(status)
                    .message(message)
                    .build();
            kafkaTemplate.send(sendNotificationTopic, sseEvent);
        } catch (Exception e) {
            log.error("Failed to send notification to user: {}", userId, e);
        }
    }
    
    @Override
    public void validateSignature(AssemblyAIWebhookRequest request, String signature) {
        // Implement webhook signature validation if AssemblyAI provides it
        // This is important for security to ensure the webhook is from AssemblyAI
        if (signature == null || signature.isEmpty()) {
            log.warn("No signature provided for webhook validation");
            return;
        }
        
        // TODO: Implement actual signature validation based on AssemblyAI documentation
        log.debug("Webhook signature validation not implemented yet");
    }
}
