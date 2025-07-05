package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @Query("""
        select q.questionId from Question q WHERE q.questionGroup.groupId = :groupId AND q.isOriginal = true
    """)
    List<UUID> findOriginalVersionByGroupId(UUID groupId);

    @Query("""
        select q from Question q
            WHERE (q.questionId IN :originalQuestionId AND q.isOriginal = true AND q.isCurrent = true)
                OR (q.parent.questionId IN :originalQuestionId AND q.isCurrent = true)
    """)
    List<Question> findAllCurrentVersion(@Param("originalQuestionId") List<UUID> originalQuestionId);
}
