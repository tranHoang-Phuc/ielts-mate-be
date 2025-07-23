package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingExamRepository extends JpaRepository<ReadingExam, UUID> {

    @Query("""
        SELECT r FROM ReadingExam r
        WHERE r.parent.readingExamId = :parentId
        AND r.isCurrent = true
    """)
    Optional<ReadingExam> findCurrentChildByParentId(@Param("parentId") UUID parentId);
    // Add this method to ReadingExamRepository.java
    List<ReadingExam> findByCreatedBy(String createdBy);

    Page<ReadingExam> findByCreatedByAndIsDeletedFalse(String createdBy, Pageable pageable);

    Page<ReadingExam> findByIsDeletedFalse(Pageable pageable);

    @Query("""
    SELECT r FROM ReadingExam r
    WHERE r.isDeleted = false
      AND r.isCurrent = true
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(r.examName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(r.examDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(r.urlSlug) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<ReadingExam> searchCurrentExams(@Param("keyword") String keyword, Pageable pageable);


    @Query("""
        SELECT r FROM ReadingExam r
        WHERE r.readingExamId = :readingExamId
        AND r.isCurrent = true
    """)
    Optional<ReadingExam> findCurrentByReadingExamId(@Param("urlSlug") String urlSlug);

    Optional<ReadingExam> findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(String urlSlug);


    @Query("""
    SELECT r FROM ReadingExam r
    WHERE r.isDeleted = false
      AND r.isCurrent = true
      AND r.status = 1
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(r.examName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(r.examDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(r.urlSlug) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<ReadingExam> findByIsDeletedFalseAndIsCurrentTrueAndStatusTrue(@Param("keyword") String keyword,Pageable pageable);
//    Optional<ReadingExam> findByParentReadingExamReadingExamIdAndIsCurrentTrue(UUID parentId);

//    Optional<ReadingPassage> findByParentPassageIdAndIsCurrentTrue(UUID parentId);
}
