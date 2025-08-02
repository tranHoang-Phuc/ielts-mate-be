package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChoiceRepository extends JpaRepository<Choice, UUID> {
    @Query("""
        select c from Choice c
            where 
                (c.choiceId IN (select ch.choiceId from Choice ch where ch.question.questionId = :questionId) and c.isOriginal = true and c.isCurrent = true and c.isDeleted =false)
                   or 
                (c.parent.choiceId in (select ch.choiceId from Choice ch where ch.question.questionId = :questionId) and c.isCurrent = true and c.isDeleted = false )
    """)
    List<Choice> findCurrentVersionByQuestionId(UUID questionId);

    @Query("""
        select c.label from Choice c where c.choiceId IN :choices
    """)
    List<String> getChoicesByIds(List<UUID> choices);

    @Query("""
    SELECT c
      FROM Choice c
     WHERE c.isOriginal = TRUE
       AND c.question.questionId = :questionId
""")
    List<Choice> getOriginalChoiceByOriginalQuestion(@Param("questionId") UUID questionId);

    @Query("""
    SELECT c
      FROM Choice c
     WHERE c.isCorrect = TRUE
       AND (c.choiceId IN :originalChoices OR c.parent.choiceId IN :originalChoices)
""")
    List<Choice> getCurrentCorrectChoice(
            @Param("originalChoices") List<UUID> originalChoices
    );

    @Query("""
        SELECT  c FROM Choice c WHERE c.question.questionId = :questionId AND c.isCurrent = true
        """)
    List<Choice> getVersionChoiceByQuestionId(UUID questionId);

    @Query("""
        SELECT  c FROM Choice c WHERE c.question.questionId = :questionId
        """)
    List<Choice> getVersionChoiceByParentQuestionId(UUID questionId);

    @Query("""
            SELECT c FROM Choice c where c.parent.choiceId = :choiceId AND c.isCurrent = true
        """)
    Choice getCurrentVersionChoiceByChoiceId(UUID choiceId);

    List<Choice> findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(Question question, boolean isDeleted, Boolean isCurrent);
}
