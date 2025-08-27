package com.fptu.sep490.listeningservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_attempt")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamAttempt {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "exam_attempt_id", updatable = false, nullable = false)
    private UUID examAttemptId;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "total_point")
    private Integer totalPoint;

    @Column(name = "history", columnDefinition = "TEXT")
    private String history;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listening_exam_id")
    private ListeningExam listeningExam;

    @Column(name = "is_finish")
    private Boolean isFinished;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
