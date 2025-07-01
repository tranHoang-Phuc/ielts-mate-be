package com.fptu.sep490.readingservice.model;

import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reading_passages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingPassage {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "passage_id", updatable = false, nullable = false)
    private UUID passageId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "ielts_type", nullable = false)
    private IeltsType ieltsType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "part_number", nullable = false)
    private PartNumber partNumber;

    @Column(name = "instruction", nullable = false, columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "title", nullable = false)
    private String title;


    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "passage_status", nullable = false)
    private Status passageStatus;


    @Column(name = "content_with_highlight_keyword", nullable = false, columnDefinition = "TEXT")
    private String contentWithHighlightKeyword;

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
            mappedBy = "readingPassage",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<QuestionGroup> questionGroups = new ArrayList<>();

    @OneToMany(
            mappedBy = "readingPassage",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Attempt> attempts = new ArrayList<>();

    @OneToMany(
            mappedBy = "part1",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ReadingExam> readingExamsPart1 = new ArrayList<>();

    @OneToMany(
            mappedBy = "part2",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ReadingExam> readingExamsPart2 = new ArrayList<>();

    @OneToMany(
            mappedBy = "part3",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ReadingExam> readingExamsPart3 = new ArrayList<>();


    @Column(name = "is_current")
    private Boolean isCurrent = true;

    @Column(name = "display_version")
    private Integer version = 1;

    @Column(name = "is_original")
    private Boolean isOriginal = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_question_parent"))
    private ReadingPassage parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReadingPassage> children = new ArrayList<>();
}
