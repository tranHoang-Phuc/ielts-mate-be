package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.event.AudioFileUpload;

public interface AsyncTranscriptService {
    void initiateTranscriptGeneration(AudioFileUpload audioFileUpload);
    void retryFailedTranscripts();
    void cleanupOldTranscriptStatuses();
}
