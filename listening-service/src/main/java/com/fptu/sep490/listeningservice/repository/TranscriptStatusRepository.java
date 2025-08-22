package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.TranscriptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TranscriptStatusRepository extends JpaRepository<TranscriptStatus, UUID> {
    
    Optional<TranscriptStatus> findByTranscriptId(UUID transcriptId);
    
    List<TranscriptStatus> findByTaskId(UUID taskId);
    
    List<TranscriptStatus> findByStatus(String status);
    
    @Query("SELECT ts FROM TranscriptStatus ts WHERE ts.status = 'processing' AND ts.createdAt < :threshold")
    List<TranscriptStatus> findStuckProcessingTranscripts(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT ts FROM TranscriptStatus ts WHERE ts.createdBy = :userId ORDER BY ts.createdAt DESC")
    List<TranscriptStatus> findByCreatedByOrderByCreatedAtDesc(@Param("userId") String userId);
}
