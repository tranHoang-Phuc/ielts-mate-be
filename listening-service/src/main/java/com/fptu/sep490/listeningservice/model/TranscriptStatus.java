package com.fptu.sep490.listeningservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transcript_status")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranscriptStatus {
    
    @Id
    @UuidGenerator
    @Column(name = "id")
    UUID id;
    
    @Column(name = "transcript_id", unique = true, nullable = false)
    UUID transcriptId;
    
    @Column(name = "task_id", nullable = false)
    UUID taskId;
    
    @Column(name = "status", nullable = false)
    String status;
    
    @Column(name = "transcript_text", columnDefinition = "TEXT")
    String transcriptText;
    
    @Column(name = "confidence")
    Double confidence;
    
    @Column(name = "audio_duration")
    Double audioDuration;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;
    
    @Column(name = "created_by", nullable = false)
    String createdBy;
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
