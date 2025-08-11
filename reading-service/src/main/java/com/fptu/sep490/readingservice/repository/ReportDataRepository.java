package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface ReportDataRepository extends JpaRepository<ReportData, Integer> {
    @Query(value = """
        SELECT question_type, COUNT(*) AS correct_count
        FROM report_data
        WHERE is_correct = true
          AND (:fromDate IS NULL OR checked_date >= :fromDate)
          AND (:toDate IS NULL OR checked_date <= :toDate)
        GROUP BY question_type
        """, nativeQuery = true)
    List<Object[]> countCorrectByQuestionType(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
