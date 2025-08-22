package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import com.fptu.sep490.listeningservice.repository.TranscriptStatusRepository;
import com.fptu.sep490.listeningservice.service.CachedTranscriptService;
import com.fptu.sep490.listeningservice.viewmodel.response.TranscriptStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
public class CachedTranscriptServiceImpl implements CachedTranscriptService {
    
    TranscriptStatusRepository transcriptStatusRepository;
    RedisTemplate<String, Object> redisTemplate;
    
    private static final String TRANSCRIPT_STATUS_CACHE = "transcript_status:";
    private static final String TASK_TRANSCRIPTS_CACHE = "task_transcripts:";
    private static final String USER_TRANSCRIPTS_CACHE = "user_transcripts:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    @Override
    @Cacheable(value = "transcript_status", key = "#transcriptId")
    public Optional<TranscriptStatusResponse> getTranscriptStatus(UUID transcriptId) {
        log.debug("Fetching transcript status for ID: {}", transcriptId);
        
        // Try cache first
        String cacheKey = TRANSCRIPT_STATUS_CACHE + transcriptId;
        try {
            TranscriptStatusResponse cached = (TranscriptStatusResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Found cached transcript status for ID: {}", transcriptId);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.warn("Failed to get transcript status from cache: {}", transcriptId, e);
        }
        
        // Fallback to database
        Optional<TranscriptStatus> transcriptStatus = transcriptStatusRepository.findByTranscriptId(transcriptId);
        if (transcriptStatus.isPresent()) {
            TranscriptStatusResponse response = mapToResponse(transcriptStatus.get());
            cacheTranscriptStatus(transcriptStatus.get());
            return Optional.of(response);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<TranscriptStatusResponse> getTranscriptStatusesByTaskId(UUID taskId) {
        log.debug("Fetching transcript statuses for task ID: {}", taskId);
        
        // Try cache first
        String cacheKey = TASK_TRANSCRIPTS_CACHE + taskId;
        try {
            @SuppressWarnings("unchecked")
            List<TranscriptStatusResponse> cached = (List<TranscriptStatusResponse>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Found cached transcript statuses for task ID: {}", taskId);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get transcript statuses from cache for task: {}", taskId, e);
        }
        
        // Fallback to database
        List<TranscriptStatus> transcriptStatuses = transcriptStatusRepository.findByTaskId(taskId);
        List<TranscriptStatusResponse> responses = transcriptStatuses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        // Cache the results
        try {
            redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache transcript statuses for task: {}", taskId, e);
        }
        
        return responses;
    }
    
    @Override
    public List<TranscriptStatusResponse> getUserTranscriptStatuses(String userId) {
        log.debug("Fetching transcript statuses for user: {}", userId);
        
        // Try cache first
        String cacheKey = USER_TRANSCRIPTS_CACHE + userId;
        try {
            @SuppressWarnings("unchecked")
            List<TranscriptStatusResponse> cached = (List<TranscriptStatusResponse>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Found cached transcript statuses for user: {}", userId);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get transcript statuses from cache for user: {}", userId, e);
        }
        
        // Fallback to database
        List<TranscriptStatus> transcriptStatuses = transcriptStatusRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        List<TranscriptStatusResponse> responses = transcriptStatuses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        // Cache the results
        try {
            redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache transcript statuses for user: {}", userId, e);
        }
        
        return responses;
    }
    
    @Override
    public void cacheTranscriptStatus(TranscriptStatus transcriptStatus) {
        TranscriptStatusResponse response = mapToResponse(transcriptStatus);
        
        String statusCacheKey = TRANSCRIPT_STATUS_CACHE + transcriptStatus.getTranscriptId();
        try {
            redisTemplate.opsForValue().set(statusCacheKey, response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache transcript status: {}", transcriptStatus.getTranscriptId(), e);
        }
        
        // Invalidate related caches
        evictRelatedCaches(transcriptStatus);
    }
    
    @Override
    @CacheEvict(value = "transcript_status", key = "#transcriptId")
    public void evictTranscriptCache(UUID transcriptId) {
        String cacheKey = TRANSCRIPT_STATUS_CACHE + transcriptId;
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Evicted transcript cache for ID: {}", transcriptId);
        } catch (Exception e) {
            log.warn("Failed to evict transcript cache: {}", transcriptId, e);
        }
    }
    
    private void evictRelatedCaches(TranscriptStatus transcriptStatus) {
        try {
            // Evict task-related cache
            String taskCacheKey = TASK_TRANSCRIPTS_CACHE + transcriptStatus.getTaskId();
            redisTemplate.delete(taskCacheKey);
            
            // Evict user-related cache
            String userCacheKey = USER_TRANSCRIPTS_CACHE + transcriptStatus.getCreatedBy();
            redisTemplate.delete(userCacheKey);
            
            log.debug("Evicted related caches for transcript: {}", transcriptStatus.getTranscriptId());
        } catch (Exception e) {
            log.warn("Failed to evict related caches for transcript: {}", transcriptStatus.getTranscriptId(), e);
        }
    }
    
    private TranscriptStatusResponse mapToResponse(TranscriptStatus transcriptStatus) {
        return TranscriptStatusResponse.builder()
                .transcriptId(transcriptStatus.getTranscriptId())
                .taskId(transcriptStatus.getTaskId())
                .status(transcriptStatus.getStatus())
                .transcriptText(transcriptStatus.getTranscriptText())
                .confidence(transcriptStatus.getConfidence())
                .audioDuration(transcriptStatus.getAudioDuration())
                .errorMessage(transcriptStatus.getErrorMessage())
                .createdAt(transcriptStatus.getCreatedAt())
                .updatedAt(transcriptStatus.getUpdatedAt())
                .build();
    }
}
