package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingExam;
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
}
