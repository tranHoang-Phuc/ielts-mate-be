package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @Query("""
        select q.questionId from Question q WHERE q.questionGroup.groupId = :groupId AND q.isOriginal = true and q.isDeleted = false
    """)
    List<UUID> findOriginalVersionByGroupId(UUID groupId);

    @Query("""
    select q
      from Question q
      left join fetch q.dragItem di
     where (q.questionId in :originalQuestionId
            and q.isOriginal = true
            and q.isCurrent = true
            and q.isDeleted = false)
        or (q.parent.questionId in :originalQuestionId
            and q.isCurrent = true
            and q.isDeleted = false)
    """)
    List<Question> findAllCurrentVersion(
            @Param("originalQuestionId") List<UUID> originalQuestionId
    );
}
