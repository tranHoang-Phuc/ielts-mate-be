package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListeningExamRepository extends JpaRepository<ListeningExam, UUID> {

    @Query("""
    SELECT le FROM ListeningExam le
    WHERE le.parent.listeningExamId = :examId
""")
    List<ListeningExam> findAllCurrentByParentId(@Param("examId") UUID examId);


    @Query("""
    SELECT le FROM ListeningExam le
    WHERE le.createdBy = :userId
    AND le.isDeleted = false
    AND le.isCurrent = true
    AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(le.examName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(le.examDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(le.urlSlug) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    Page<ListeningExam> searchCurrentExamsByCreator(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
    SELECT le FROM ListeningExam le
    WHERE le.isDeleted = false
    AND le.isCurrent = true
    AND le.status = 0
    AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(le.examName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(le.examDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(le.urlSlug) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    Page<ListeningExam> searchCurrentExamsActivated(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<ListeningExam> findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(String urlSlug);

    @Query("""
        SELECT l FROM ListeningExam l
        WHERE l.parent.listeningExamId = :parentId
        AND l.isCurrent = true
    """)
    Optional<ListeningExam> findCurrentChildByParentId(@Param("parentId") UUID parentId);
}
