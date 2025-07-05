package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.AnswerAttempt;
import com.fptu.sep490.listeningservice.model.embedded.AnswerAttemptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, AnswerAttemptId> {

    AnswerAttempt findAnswerAttemptById(AnswerAttemptId id);
}
