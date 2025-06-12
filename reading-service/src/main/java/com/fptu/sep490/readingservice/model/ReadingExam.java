package com.fptu.sep490.readingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reading_exam")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReadingExam {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "reading_exam_id", updatable = false, nullable = false)
    private UUID readingExamId;

    @Column(name = "exam_name", nullable = false, length = 255)
    private String examName;

    @Column(name = "url_slug", nullable = false, length = 255, unique = true)
    private String urlSlug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part1_id", nullable = false)
    private ReadingPassage part1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part2_id", nullable = false)
    private ReadingPassage part2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part3_id", nullable = false)
    private ReadingPassage part3;


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

    @OneToMany(
            mappedBy = "readingExam",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ExamAttempt> examAttempts = new ArrayList<>();

    @Column(name = "is_current")
    private Boolean isCurrent = true;

    @Column(name = "display_version")
    private Integer version = 1;

    @Column(name = "is_original", nullable = false)
    private boolean isOriginal = true;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            unique = true,
            foreignKey = @ForeignKey(name = "fk_reading_exam_parent")
    )
    private ReadingExam parent;

    @OneToOne(
            mappedBy = "parent",
            fetch = FetchType.LAZY
    )
    private ReadingExam child;

}
