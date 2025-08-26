package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho bảng questions.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findAllByQuestionGroupOrderByQuestionOrderAsc(QuestionGroup questionGroup);

    @Query("""
        SELECT q FROM Question q JOIN QuestionGroup qg ON q.questionGroup = qg
        WHERE qg.readingPassage = :readingPassage
    """)
    List<Question> findAllByReadingPassage(@Param("readingPassage")ReadingPassage readingPassage);

    @Query("""
    SELECT q FROM Question q
    WHERE q.isCurrent = true AND (q.parent IN :originQuestions OR q.parent IS NULL)
    """)
    List<Question> findCurrentVersionByOriginQuestions(@Param("originQuestions") List<Question> originQuestions);

    @Query("""
        SELECT q FROM Question q
            WHERE q.questionId = :questionId OR q.parent.questionId = :questionId
    """)
    List<Question> findAllPreviousVersion(UUID questionId);

    @Query("""
        SELECT q FROM Question q
        WHERE q.questionId = :questionId OR q.parent.questionId = :questionId
    """)
    List<Question> findAllVersionByQuestionId(Question question);

    @Query("""
        SELECT q FROM Question q  LEFT JOIN FETCH Question q2 ON q.parent = q2
        WHERE q.questionGroup.groupId = :groupId AND q.isCurrent = true
    """)
    List<Question> findCurrentVersionByGroup(UUID groupId);

    @Query("""
        SELECT q FROM Question q
        WHERE q.questionId IN :collect
    """)
    List<Question> findQuestionsByIds(List<UUID> collect);

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
    List<Question> findAllCurrentVersion(List<UUID> originalQuestionId);

    @Query("""
        select q.questionId from Question q where q.questionGroup.groupId =: groupId and q.isOriginal = true
    """)
    List<UUID> findQuestionsByGroupId(@Param("groupId") UUID groupId);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Question q where q.questionGroup = :group")
    List<Question> lockAllByGroup(@Param("group") QuestionGroup group);

    @Query("""
        select q from Question q
        where (q.parent is null and q.questionId in :baseIds and q.isCurrent = true)
           or (q.parent is not null and q.parent.questionId in :baseIds and q.isCurrent = true)
        """)
    List<Question> findAllCurrentVersion(@Param("baseIds") Collection<UUID> baseIds);

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
