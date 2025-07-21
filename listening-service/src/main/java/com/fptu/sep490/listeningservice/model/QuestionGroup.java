package com.fptu.sep490.listeningservice.model;

import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
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
@Table(name = "question_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionGroup {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "group_id", updatable = false, nullable = false)
    private UUID groupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private ListeningTask listeningTask;

    @Column(name = "section_order")
    private Integer sectionOrder;

    @Column(name = "section_label", length = 255)
    private String sectionLabel;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE; // default value


    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

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

    @Column(name = "is_current")
    private Boolean isCurrent = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "display_version")
    private Integer version = 1;

    @Column(name = "is_original")
    private Boolean isOriginal = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_group_parent"))
    private QuestionGroup parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<QuestionGroup> children = new ArrayList<>();

    @OneToMany(
            mappedBy = "questionGroup",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DragItem> dragItems = new ArrayList<>();
}
