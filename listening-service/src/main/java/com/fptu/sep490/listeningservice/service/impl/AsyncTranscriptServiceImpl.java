package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.repository.TranscriptStatusRepository;
import com.fptu.sep490.listeningservice.repository.client.AssemblyAIClient;
import com.fptu.sep490.listeningservice.service.AsyncTranscriptService;
import com.fptu.sep490.listeningservice.viewmodel.request.GenTranscriptRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
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
public class AsyncTranscriptServiceImpl implements AsyncTranscriptService {
    
    ListeningTaskRepository listeningTaskRepository;
    TranscriptStatusRepository transcriptStatusRepository;
    AssemblyAIClient assemblyAIClient;
    KafkaTemplate<String, Object> kafkaTemplate;
    RedisTemplate<String, Object> redisTemplate;
    
    @Value("${assembly-ai.api-key}")
    @NonFinal
    String assemblyAIApiKey;
    
    @Value("${webhook.base-url}")
    @NonFinal
    String webhookBaseUrl;
    
    @Value("${topic.send-notification}")
    @NonFinal
    String sendNotificationTopic;
    
    private static final String TRANSCRIPT_PROCESSING_KEY = "transcript:processing:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    
    @Override
    @Async("transcriptExecutor")
    @Transactional
    @Retryable(
        retryFor = {Exception.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void initiateTranscriptGeneration(AudioFileUpload audioFileUpload) {
        String publicUrl = audioFileUpload.getPublicUrl();
        UUID taskId = audioFileUpload.getTaskId();
        
        log.info("Initiating transcript generation for task ID: {} with audio URL: {}", taskId, publicUrl);
        
        // Get the listening task to get the creator
        List<ListeningTask> allVersions = listeningTaskRepository.findAllVersion(taskId);
        if (allVersions.isEmpty()) {
            log.error("No listening task found for task ID: {}", taskId);
            return;
        }
        
        ListeningTask listeningTask = allVersions.get(0);
        String webhookUrl = webhookBaseUrl + "/listening/webhooks/assembly-ai/transcript";
        
        GenTranscriptRequest requestBody = GenTranscriptRequest.builder()
                .audioUrl(publicUrl)
                .languageCode(Constants.AssemblyAI.LANGUAGE_CODE_EN)
                .speakerLabels(Constants.AssemblyAI.SPEAKER_LABELS)
                .webhookUrl(webhookUrl)
                .build();
        
        try {
            // Send transcript request to AssemblyAI
            var transcriptRequest = assemblyAIClient.createGenTranscriptRequest(requestBody, assemblyAIApiKey);
            
            if (transcriptRequest.getStatusCode() == HttpStatus.OK && transcriptRequest.getBody() != null) {
                UUID transcriptId = transcriptRequest.getBody().id();
                
                // Create transcript status record
                TranscriptStatus transcriptStatus = TranscriptStatus.builder()
                        .transcriptId(transcriptId)
                        .taskId(taskId)
                        .status("queued")
                        .createdBy(listeningTask.getCreatedBy())
                        .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                        .updatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                        .build();
                
                transcriptStatusRepository.save(transcriptStatus);
                
                // Cache the processing status
                cacheTranscriptProcessing(transcriptId, taskId);
                
                // Send notification that processing has started
                sendProcessingNotification(listeningTask.getCreatedBy());
                
                log.info("Successfully initiated transcript generation for task ID: {} with transcript ID: {}", 
                        taskId, transcriptId);
                
            } else {
                log.error("Failed to create transcript request for task ID: {}. Status: {}", 
                        taskId, transcriptRequest.getStatusCode());
                
                sendErrorNotification(listeningTask.getCreatedBy(), 
                        "Failed to start audio transcription processing");
                
                throw new AppException(
                        Constants.ErrorCodeMessage.CREATE_GEN_TRANSCRIPT_ERROR,
                        Constants.ErrorCode.CREATE_GEN_TRANSCRIPT_ERROR, 
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
        } catch (Exception e) {
            log.error("Error initiating transcript generation for task ID: {}", taskId, e);
            sendErrorNotification(listeningTask.getCreatedBy(), 
                    "An error occurred while starting audio transcription");
            throw e;
        }
    }
    
    @Override
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void retryFailedTranscripts() {
        log.debug("Checking for stuck processing transcripts...");
        
        LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(30);
        List<TranscriptStatus> stuckTranscripts = transcriptStatusRepository
                .findStuckProcessingTranscripts(threshold);
        
        for (TranscriptStatus transcript : stuckTranscripts) {
            log.warn("Found stuck transcript: {} for task: {}", 
                    transcript.getTranscriptId(), transcript.getTaskId());
            
            try {
                // Try to get the current status from AssemblyAI
                var response = assemblyAIClient.getTranscript(transcript.getTranscriptId(), assemblyAIApiKey);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    var responseBody = response.getBody();
                    String currentStatus = responseBody.status();
                    
                    if ("completed".equalsIgnoreCase(currentStatus)) {
                        // Update the transcript with the completed data
                        handleStuckCompletedTranscript(transcript, responseBody);
                    } else if ("error".equalsIgnoreCase(currentStatus)) {
                        transcript.setStatus("error");
                        transcript.setErrorMessage("Transcript processing failed");
                        transcriptStatusRepository.save(transcript);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking stuck transcript: {}", transcript.getTranscriptId(), e);
            }
        }
    }
    
    @Override
    @Scheduled(fixedDelay = 86400000) // Run daily
    public void cleanupOldTranscriptStatuses() {
        log.info("Cleaning up old transcript statuses...");
        
        // TODO: Implement cleanup logic for old transcript statuses
        // This would require a custom query to delete old records
        // Implementation depends on your cleanup requirements
        
        log.info("Transcript status cleanup completed");
    }
    
    private void handleStuckCompletedTranscript(TranscriptStatus transcript, 
                                               com.fptu.sep490.listeningservice.viewmodel.response.TranscriptResponse response) {
        transcript.setStatus("completed");
        transcript.setTranscriptText(response.text());
        transcript.setConfidence(response.confidence());
        transcript.setAudioDuration(response.audioDuration());
        transcript.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        
        // Update all versions of the listening task
        List<ListeningTask> allVersions = listeningTaskRepository.findAllVersion(transcript.getTaskId());
        for (ListeningTask listeningTask : allVersions) {
            listeningTask.setTranscription(response.text());
        }
        listeningTaskRepository.saveAll(allVersions);
        transcriptStatusRepository.save(transcript);
        
        // Send success notification
        sendSuccessNotification(transcript.getCreatedBy());
        
        log.info("Successfully recovered stuck transcript: {} for task: {}", 
                transcript.getTranscriptId(), transcript.getTaskId());
    }
    
    private void cacheTranscriptProcessing(UUID transcriptId, UUID taskId) {
        String cacheKey = TRANSCRIPT_PROCESSING_KEY + transcriptId;
        try {
            redisTemplate.opsForValue().set(cacheKey, taskId.toString(), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache transcript processing status for ID: {}", transcriptId, e);
        }
    }
    
    private void sendProcessingNotification(String userId) {
        sendNotification(userId, "info", "Your audio is being processed for transcription. You will be notified when it's ready.");
    }
    
    private void sendSuccessNotification(String userId) {
        sendNotification(userId, "success", "Your audio transcription has been completed successfully.");
    }
    
    private void sendErrorNotification(String userId, String message) {
        sendNotification(userId, "error", message);
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
}
