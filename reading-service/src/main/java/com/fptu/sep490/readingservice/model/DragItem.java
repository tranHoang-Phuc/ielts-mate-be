package com.fptu.sep490.readingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "drag_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DragItem {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "drag_item_id", updatable = false, nullable = false)
    private UUID dragItemId;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private QuestionGroup questionGroup;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "question_id",
            unique = true         // đảm bảo mỗi question_id chỉ dùng cho đúng 1 DragItem
    )
    private Question question;

    @Column(name = "is_current")
    private Boolean isCurrent = true;

    @Column(name = "display_version")
    private Integer version = 1;
    @Column(name = "is_original")
    private Boolean isOriginal = true;
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_dragItem_parent"))
    private DragItem parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DragItem> children = new ArrayList<>();
}
