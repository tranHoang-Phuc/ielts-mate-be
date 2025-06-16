package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChoiceRepository extends JpaRepository<Choice, UUID> {
    List<Choice> findByQuestion(Question question);

    @Query("""
        SELECT c FROM Choice c JOIN Question q ON c.question = q
        WHERE q = :q AND c.isCorrect = true
    """)
    List<Choice> findCorrectChoiceByQuestion(Question q);
}
