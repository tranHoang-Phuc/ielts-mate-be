package com.fptu.sep490.readingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "attempt_id", updatable = false, nullable = false)
    private UUID attemptId;

    @Column(name = "duration")
    private long duration;

    @Column(name="total_points")
    private int totalPoints;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name ="finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_passage_id", nullable = false)
    private ReadingPassage readingPassage;
}
