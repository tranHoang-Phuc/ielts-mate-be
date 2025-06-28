package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.AnswerAttempt;
import com.fptu.sep490.readingservice.model.Attempt;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, AnswerAttemptId> {
    AnswerAttempt findAnswerAttemptById(AnswerAttemptId id);

    List<AnswerAttempt> findByAttempt(Attempt attempt);

    @Query("""
        SELECT aa.question FROM AnswerAttempt aa JOIN FETCH Question q ON aa.question.questionId = q.questionId
        WHERE aa.attempt.attemptId = :attemptId
    """)
    List<Question> findAllQuestionsByAttempt(UUID attemptId);

    @Query("""
        select at from AnswerAttempt at where at.id = :id
    """)
    Optional<AnswerAttempt> findAnswerAttemptByAttemptId(AnswerAttemptId id);
}
