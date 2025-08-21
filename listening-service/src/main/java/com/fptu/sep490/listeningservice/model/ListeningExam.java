package com.fptu.sep490.listeningservice.model;


import com.fptu.sep490.listeningservice.model.enumeration.ExamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listening_exam")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListeningExam {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "listening_exam_id", updatable = false, nullable = false)
    private UUID listeningExamId;

    @Column(name = "exam_name", nullable = false, length = 255)
    private String examName;

    @Column(name = "exam_description", columnDefinition = "TEXT")
    private String examDescription;

    @Column(name = "url_slug", nullable = false, length = 255, unique = true)
    private String urlSlug;

    @Enumerated(EnumType.ORDINAL)
    @Column(name ="status")
    private ExamStatus status = ExamStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part1_id", nullable = false)
    private ListeningTask part1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part2_id", nullable = false)
    private ListeningTask part2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part3_id", nullable = false)
    private ListeningTask part3;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part4_id", nullable = false)
    private ListeningTask part4;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
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
    private ListeningExam parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ListeningExam> children = new ArrayList<>();

    @OneToMany(
            mappedBy = "listeningExam",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ExamAttempt> examAttempts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
