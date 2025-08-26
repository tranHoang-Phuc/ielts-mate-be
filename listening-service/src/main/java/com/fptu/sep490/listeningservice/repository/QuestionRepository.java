package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
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
    @Query("""
        SELECT q FROM Question q
        WHERE q.questionId IN :collect
    """)
    List<Question> findQuestionsByIds(List<UUID> collect);

    List<Question> findAllByQuestionGroupOrderByQuestionOrderAsc(QuestionGroup questionGroup);

    @Query("""
        SELECT q FROM Question q
        WHERE q.questionId = :questionId OR q.parent.questionId = :questionId
    """)
    List<Question> findAllVersionByQuestionId(Question question);

    @Query("""
        SELECT q FROM Question q
            WHERE q.questionId = :questionId OR q.parent.questionId = :questionId
    """)
    List<Question> findAllPreviousVersion(UUID questionId);

    @Query("""
        SELECT q FROM Question q  LEFT JOIN FETCH Question q2 ON q.parent = q2
        WHERE q.questionGroup.groupId = :groupId AND q.isCurrent = true
    """)
    List<Question> findCurrentVersionByGroup(UUID groupId);

    @Query("""
        SELECT q FROM Question q
        WHERE q.questionId IN :ids
        ORDER BY q.questionOrder ASC
    """)
    List<Question> findAllByIdOrderByQuestionOrder(@Param("ids") List<UUID> ids);
    @Query("""
        SELECT q FROM Question q
        WHERE ( q.questionId = :questionId  OR q.parent.questionId = :questionId ) AND q.isCurrent = true
    """)
    Question findCurrentQuestion(UUID questionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Question q
           set q.questionOrder = :newOrder,
               q.updatedBy = :updatedBy
         where (q.questionId = :baseId or q.parent.questionId = :baseId)
        """)
    int updateOrderForAllVersions(@Param("baseId") UUID baseId,
                                  @Param("newOrder") int newOrder,
                                  @Param("updatedBy") String updatedBy);

    @Query("""
        select q from Question q 
        left join fetch q.categories 
        where q.questionId = :questionId
        """)
    Optional<Question> findByIdWithCategories(@Param("questionId") UUID questionId);
}
