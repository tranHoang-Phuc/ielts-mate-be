package com.fptu.sep490.readingservice.model;

import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name ="questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @Column(name = "question_order")
    private int questionOrder;

    @Column(name = "point")
    private int point;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "question_categories",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "category", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<QuestionCategory> categories = new HashSet<>();

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "number_of_correct_answers")
    private int numberOfCorrectAnswers;

    @Column(name = "instruction_for_choice", length = 1000)
    private String instructionForChoice;

    @Column(name = "blank_index")
    private Integer blankIndex;

    @Column(name = "correct_answer", length = 50)
    private String correctAnswer;

    @Column(name = "instruction_for_matching", length = 1000)
    private String instructionForMatching;

    @Column(name = "correct_answer_for_matching", length = 50)
    private String correctAnswerForMatching;

    @Column(name = "zone_index")
    private Integer zoneIndex;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    private QuestionGroup questionGroup;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Choice> choices = new ArrayList<>();

    @OneToOne(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY,
            optional = false    // bảo đảm Question luôn có DragItem
    )
    private DragItem dragItem;
}
