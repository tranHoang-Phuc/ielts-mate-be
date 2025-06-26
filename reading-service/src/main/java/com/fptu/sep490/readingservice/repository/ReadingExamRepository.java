package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReadingExamRepository extends JpaRepository<ReadingExam, UUID> {

    @Query("""
        SELECT r FROM ReadingExam r
        WHERE r.parent.readingExamId = :parentId
        AND r.isCurrent = true
    """)
    Optional<ReadingExam> findCurrentChildByParentId(@Param("parentId") UUID parentId);


    @Query("""
        SELECT r FROM ReadingExam r
        WHERE r.readingExamId = :readingExamId
        AND r.isCurrent = true
    """)
    Optional<ReadingExam> findCurrentByReadingExamId(@Param("urlSlug") String urlSlug);

    Optional<ReadingExam> findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(String urlSlug);
//    Optional<ReadingExam> findByParentReadingExamReadingExamIdAndIsCurrentTrue(UUID parentId);

//    Optional<ReadingPassage> findByParentPassageIdAndIsCurrentTrue(UUID parentId);
}
