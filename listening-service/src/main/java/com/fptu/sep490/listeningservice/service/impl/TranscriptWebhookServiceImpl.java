package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.repository.TranscriptStatusRepository;
import com.fptu.sep490.listeningservice.service.TranscriptWebhookService;
import com.fptu.sep490.listeningservice.viewmodel.request.AssemblyAIWebhookRequest;
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
    
    @Value("${topic.send-notification}")
    @NonFinal
    String sendNotificationTopic;
    
    private static final String TRANSCRIPT_CACHE_PREFIX = "transcript:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    @Override
    @Transactional
    public void processTranscriptWebhook(AssemblyAIWebhookRequest webhookRequest) {
        UUID transcriptId = webhookRequest.transcriptId();
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
        String transcriptText = webhookRequest.text();
        
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
        
        log.info("Successfully processed completed transcript for task ID: {}", taskId);
    }
    
    private void handleErrorTranscript(AssemblyAIWebhookRequest webhookRequest, TranscriptStatus transcriptStatus) {
        transcriptStatus.setErrorMessage(webhookRequest.error());
        
        // Send error notification
        sendNotification(transcriptStatus.getCreatedBy(), "error", 
                "There was an error processing your audio transcription: " + webhookRequest.error());
        
        log.error("Transcript processing failed for ID: {} with error: {}", 
                webhookRequest.transcriptId(), webhookRequest.error());
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
