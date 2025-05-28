package com.fptu.sep490.readingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "choices")
public class Choice {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "choice_id", updatable = false, nullable = false)
    private UUID choiceId;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "choice_order", nullable = false)
    private int choiceOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
}
