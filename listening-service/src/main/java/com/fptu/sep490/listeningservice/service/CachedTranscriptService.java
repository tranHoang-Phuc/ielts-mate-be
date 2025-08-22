package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import com.fptu.sep490.listeningservice.viewmodel.response.TranscriptStatusResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CachedTranscriptService {
    Optional<TranscriptStatusResponse> getTranscriptStatus(UUID transcriptId);
    List<TranscriptStatusResponse> getTranscriptStatusesByTaskId(UUID taskId);
    List<TranscriptStatusResponse> getUserTranscriptStatuses(String userId);
    void cacheTranscriptStatus(TranscriptStatus transcriptStatus);
    void evictTranscriptCache(UUID transcriptId);
}
