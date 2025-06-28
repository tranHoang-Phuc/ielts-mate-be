package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChoiceRepository extends JpaRepository<Choice, UUID> {
    List<Choice> findByQuestionAndIsDeletedOrderByChoiceOrderAsc(Question question, Boolean isDeleted);

    @Query("""
        SELECT c FROM Choice c JOIN Question q ON c.question = q
        WHERE q = :q AND c.isCorrect = true
    """)
    List<Choice> findCorrectChoiceByQuestion(Question q);

    @Query("""
        SELECT c FROM Choice c
        WHERE c.choiceId = :choiceId OR c.parent.choiceId = :choiceId
    """)
    List<Choice> findAllVersion(UUID choiceId);

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


    @Query("""
            SELECT c from Choice c WHERE c.isOriginal = true and c.question.questionId =: questionId
        """)
    List<Choice> getOriginalChoiceByOriginalQuestion(@Param("questionId") UUID questionId);

    @Query("""
        select c FROM Choice c WHERE c.isCorrect = true AND (c in :ooriginalChoice or c.parent in :originalChoice)
    """)
    List<Choice> getCurrentCorrectChoice(List<Choice> originalChoice);

    @Query("""
        select c.label from Choice c where c.choiceId IN :choices
    """)
    List<String> getChoicesByIds(List<UUID> choices);
}
