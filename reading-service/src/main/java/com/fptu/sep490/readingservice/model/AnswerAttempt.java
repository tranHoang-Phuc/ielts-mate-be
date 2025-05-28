package com.fptu.sep490.readingservice.model;

import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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

    @MapsId("readingPassageId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_passage_id", nullable = false)
    private ReadingPassage readingPassage;

    @Column(name = "isCorrect")
    private boolean isCorrect;

    @Column(name = "labal")
    private String label;

    @Column(name = "data_filled")
    private String dataFilled;

    @Column(name = "data_matched")
    private String dataMatched;

    @Column(name = "drag_item_id")
    private UUID dragItemId;
}
