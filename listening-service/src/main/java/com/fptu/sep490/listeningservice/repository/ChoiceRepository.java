package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.Choice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChoiceRepository extends JpaRepository<Choice, UUID> {
    @Query("""
        select c from Choice c
            where 
                (c.choiceId IN (select ch.choiceId from Choice ch where ch.question.questionId = :questionId) and c.isOriginal = true and c.isCurrent = true)
                   or 
                (c.parent.choiceId in (select ch.choiceId from Choice ch where ch.question.questionId = :questionId) and c.isCurrent = true)
    """)
    List<Choice> findCurrentVersionByQuestionId(UUID questionId);
}
