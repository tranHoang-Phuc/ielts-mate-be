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

    @Column(name = "exam_description", columnDefinition = "TEXT")
    private String examDescription;

    // t update lai la false vi minh update la duplicate dong
    @Column(name = "url_slug", nullable = false, length = 255, unique = false)
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

    @Column(name="status")
    private int status = 1; // Default status is true (active)

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

    @Column(name = "is_original")
    private Boolean isOriginal = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_question_parent"))
    private ReadingExam parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReadingExam> children = new ArrayList<>();
}
