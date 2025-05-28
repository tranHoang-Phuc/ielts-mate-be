package com.fptu.sep490.readingservice.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnswerAttemptId implements Serializable {

    @Column(name = "attempt_id", updatable = false, nullable = false)
    private UUID attemptId;

    @Column(name = "reading_passage_id", updatable = false, nullable = false)
    private UUID readingPassageId;
}
