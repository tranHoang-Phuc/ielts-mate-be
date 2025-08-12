package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.listeningservice.model.ExamAttempt;
import com.fptu.sep490.listeningservice.model.UserInBranch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
            @Param("startDate") LocalDateTime  startDate,
            @Param("endDate")   LocalDateTime  endDate
    );

    @Query(value = "select count(*) from exam_attempt et where et.total_point > 0 ", nativeQuery = true)
    int getNumberOfExamAttempts();
    @Query(value = "WITH all_branches AS (\n" +
            "    SELECT '9.0' AS branchScore\n" +
            "    UNION ALL SELECT '8.5'\n" +
            "    UNION ALL SELECT '8.0'\n" +
            "    UNION ALL SELECT '7.5'\n" +
            "    UNION ALL SELECT '7.0'\n" +
            "    UNION ALL SELECT '6.5'\n" +
            "    UNION ALL SELECT '6.0'\n" +
            "    UNION ALL SELECT '5.5'\n" +
            "    UNION ALL SELECT '5.0'\n" +
            "    UNION ALL SELECT '4.5'\n" +
            "    UNION ALL SELECT '4.0'\n" +
            "    UNION ALL SELECT '<4.0'\n" +
            "),\n" +
            "avg_score_per_user AS (\n" +
            "    SELECT\n" +
            "        created_by,\n" +
            "        AVG(total_point) AS avg_total_point\n" +
            "    FROM exam_attempt\n" +
            "    GROUP BY created_by\n" +
            "),\n" +
            "score_mapping AS (\n" +
            "    SELECT\n" +
            "        CASE\n" +
            "            WHEN avg_total_point BETWEEN 39 AND 40 THEN '9.0'\n" +
            "            WHEN avg_total_point BETWEEN 37 AND 38 THEN '8.5'\n" +
            "            WHEN avg_total_point BETWEEN 35 AND 36 THEN '8.0'\n" +
            "            WHEN avg_total_point BETWEEN 33 AND 34 THEN '7.5'\n" +
            "            WHEN avg_total_point BETWEEN 30 AND 32 THEN '7.0'\n" +
            "            WHEN avg_total_point BETWEEN 27 AND 29 THEN '6.5'\n" +
            "            WHEN avg_total_point BETWEEN 23 AND 26 THEN '6.0'\n" +
            "            WHEN avg_total_point BETWEEN 19 AND 22 THEN '5.5'\n" +
            "            WHEN avg_total_point BETWEEN 15 AND 18 THEN '5.0'\n" +
            "            WHEN avg_total_point BETWEEN 13 AND 14 THEN '4.5'\n" +
            "            WHEN avg_total_point BETWEEN 10 AND 12 THEN '4.0'\n" +
            "            ELSE '<4.0'\n" +
            "        END AS branchScore\n" +
            "    FROM avg_score_per_user\n" +
            ")\n" +
            "SELECT\n" +
            "    b.branchScore,\n" +
            "    COALESCE(COUNT(sm.branchScore), 0) AS numberOfUsers\n" +
            "FROM all_branches b\n" +
            "LEFT JOIN score_mapping sm\n" +
            "    ON b.branchScore = sm.branchScore\n" +
            "GROUP BY b.branchScore\n" +
            "ORDER BY b.branchScore DESC;\n", nativeQuery = true)
    List<UserInBranch> getNumberOfUsersInBranchAvg();

    @Query(value = """
        WITH branches AS (
            SELECT '9.0' AS branchScore
            UNION ALL SELECT '8.5'
            UNION ALL SELECT '8.0'
            UNION ALL SELECT '7.5'
            UNION ALL SELECT '7.0'
            UNION ALL SELECT '6.5'
            UNION ALL SELECT '6.0'
            UNION ALL SELECT '5.5'
            UNION ALL SELECT '5.0'
            UNION ALL SELECT '4.5'
            UNION ALL SELECT '4.0'
            UNION ALL SELECT '<4.0'
        ),
        best_score_per_user AS (
            SELECT 
                created_by,
                MAX(total_point) AS best_total_point
            FROM exam_attempt
            GROUP BY created_by
        ),
        branch_counts AS (
            SELECT 
                CASE 
                    WHEN best_total_point BETWEEN 39 AND 40 THEN '9.0'
                    WHEN best_total_point BETWEEN 37 AND 38 THEN '8.5'
                    WHEN best_total_point BETWEEN 35 AND 36 THEN '8.0'
                    WHEN best_total_point BETWEEN 33 AND 34 THEN '7.5'
                    WHEN best_total_point BETWEEN 30 AND 32 THEN '7.0'
                    WHEN best_total_point BETWEEN 27 AND 29 THEN '6.5'
                    WHEN best_total_point BETWEEN 23 AND 26 THEN '6.0'
                    WHEN best_total_point BETWEEN 19 AND 22 THEN '5.5'
                    WHEN best_total_point BETWEEN 15 AND 18 THEN '5.0'
                    WHEN best_total_point BETWEEN 13 AND 14 THEN '4.5'
                    WHEN best_total_point BETWEEN 10 AND 12 THEN '4.0'
                    ELSE '<4.0'
                END AS branchScore,
                COUNT(*) AS numberOfUsers
            FROM best_score_per_user
            GROUP BY branchScore
        )
        SELECT 
            b.branchScore,
            COALESCE(c.numberOfUsers, 0) AS numberOfUsers
        FROM branches b
        LEFT JOIN branch_counts c ON b.branchScore = c.branchScore
        ORDER BY 
            CASE b.branchScore
                WHEN '9.0' THEN 1
                WHEN '8.5' THEN 2
                WHEN '8.0' THEN 3
                WHEN '7.5' THEN 4
                WHEN '7.0' THEN 5
                WHEN '6.5' THEN 6
                WHEN '6.0' THEN 7
                WHEN '5.5' THEN 8
                WHEN '5.0' THEN 9
                WHEN '4.5' THEN 10
                WHEN '4.0' THEN 11
                ELSE 12
            END
    """, nativeQuery = true)
    List<UserInBranch> getNumberOfUsersInBranchHighest();
}
