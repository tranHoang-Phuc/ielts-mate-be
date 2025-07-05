package com.fptu.sep490.listeningservice.model;

import com.fptu.sep490.listeningservice.model.embedded.AnswerAttemptId;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerAttempt {
    @EmbeddedId
    private AnswerAttemptId id;

    @MapsId("attemptId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @MapsId("questionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "choices")
    private List<UUID> choices;

    @Column(name = "data_filled")
    private String dataFilled;

    @Column(name = "data_matched")
    private String dataMatched;

    @Column(name = "drag_item_id")
    private UUID dragItemId;
}
