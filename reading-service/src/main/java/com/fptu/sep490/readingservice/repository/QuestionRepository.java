package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
