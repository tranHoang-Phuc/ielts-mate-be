package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ReportData;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStatsWrong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportDataRepository extends JpaRepository<ReportData, Integer> {
    @Query(value = """
        WITH types AS (
            SELECT 0 AS questionType
            UNION ALL SELECT 1
            UNION ALL SELECT 2
            UNION ALL SELECT 3
        )
        SELECT 
            t.questionType AS questionType, 
            COALESCE(COUNT(r.id), 0) AS correctCount
        FROM types t
        LEFT JOIN report_data r 
            ON r.question_type = t.questionType
            AND r.is_correct = true
            AND (:fromDate IS NULL OR r.checked_date >= :fromDate)
            AND (:toDate IS NULL OR r.checked_date <= :toDate)
        GROUP BY t.questionType
        ORDER BY t.questionType
        """, nativeQuery = true)
    List<ReportQuestionTypeStats> countCorrectByQuestionType(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
        WITH types AS (
            SELECT 0 AS questionType
            UNION ALL SELECT 1
            UNION ALL SELECT 2
            UNION ALL SELECT 3
        )
        SELECT 
            t.questionType AS questionType, 
            COALESCE(COUNT(r.id), 0) AS wrongCount
        FROM types t
        LEFT JOIN report_data r 
            ON r.question_type = t.questionType
            AND r.is_correct = false
            AND (:fromDate IS NULL OR r.checked_date >= :fromDate)
            AND (:toDate IS NULL OR r.checked_date <= :toDate)
        GROUP BY t.questionType
        ORDER BY t.questionType
        """, nativeQuery = true)
    List<ReportQuestionTypeStatsWrong> countWrongByQuestionType(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
