package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ExamAttempt;
import com.fptu.sep490.readingservice.model.ReadingExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    Page<ExamAttempt> findAll(Specification<ExamAttempt> spec, Pageable pageable);

    @Query("SELECT e FROM ExamAttempt e WHERE e.createdBy = :userId AND e.totalPoint IS NOT NULL")
    List<ExamAttempt> findAllByUserId(String userId);

    @Query("""
      SELECT e
      FROM ExamAttempt e
      WHERE e.createdBy   = :userId
        AND e.createdAt >= COALESCE(:startDate, e.createdAt)
        AND e.createdAt <= COALESCE(:endDate,   e.createdAt)
        AND e.totalPoint IS NOT NULL
      ORDER BY e.createdAt
    """)
    List<ExamAttempt> findByUserAndDateRange(
            @Param("userId")    String         userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime  endDate
    );

    @Query(value = "select count(*) from exam_attempt et where et.total_point > 0 ", nativeQuery = true)
    int getNumberOfExamAttempts();
    @Query(value = """
    SELECT * 
    FROM exam_attempt e 
    WHERE e.created_by = :userId 
      AND e.created_at BETWEEN DATE_TRUNC('month', CURRENT_DATE) 
                          AND (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' - INTERVAL '1 second')
    """, nativeQuery = true)
    List<ExamAttempt> findAIDataInCurrentMonth(@Param("userId") String userId);
}
