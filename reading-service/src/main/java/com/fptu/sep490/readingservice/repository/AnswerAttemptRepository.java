package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.AnswerAttempt;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, AnswerAttemptId> {
    AnswerAttempt findAnswerAttemptById(AnswerAttemptId id);
}
