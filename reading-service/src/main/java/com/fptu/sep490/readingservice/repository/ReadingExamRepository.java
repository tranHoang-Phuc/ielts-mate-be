package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingExam;
import com.fptu.sep490.readingservice.model.UserInBranch;
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
    WHERE r.isDeleted = false
      AND r.isCurrent = true
      
""")
    Page<ReadingExam> searchCurrentExamsNotKeyword( Pageable pageable);

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

    @Query("""
        SELECT COUNT(distinct r.urlSlug) FROM ReadingExam r
        WHERE r.isDeleted = false
        AND r.isCurrent = true
        AND r.status = 1
    """)
    Integer numberOfActiveExams();

    @Query(value = "select count(*) from reading_exam where is_original = true and is_deleted = false", nativeQuery = true)
    int getNumberOfExams();

    @Query(value = """
        WITH score_branches AS (
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
        avg_score_per_user AS (
            SELECT
                created_by,
                AVG(total_point) AS avg_total_point
            FROM exam_attempt
            GROUP BY created_by
        ),
        user_branch AS (
            SELECT
                CASE
                    WHEN avg_total_point BETWEEN 39 AND 40 THEN '9.0'
                    WHEN avg_total_point BETWEEN 37 AND 38 THEN '8.5'
                    WHEN avg_total_point BETWEEN 35 AND 36 THEN '8.0'
                    WHEN avg_total_point BETWEEN 33 AND 34 THEN '7.5'
                    WHEN avg_total_point BETWEEN 30 AND 32 THEN '7.0'
                    WHEN avg_total_point BETWEEN 27 AND 29 THEN '6.5'
                    WHEN avg_total_point BETWEEN 23 AND 26 THEN '6.0'
                    WHEN avg_total_point BETWEEN 19 AND 22 THEN '5.5'
                    WHEN avg_total_point BETWEEN 15 AND 18 THEN '5.0'
                    WHEN avg_total_point BETWEEN 13 AND 14 THEN '4.5'
                    WHEN avg_total_point BETWEEN 10 AND 12 THEN '4.0'
                    ELSE '<4.0'
                END AS branchScore
            FROM avg_score_per_user
        )
        SELECT
            sb.branchScore,
            COALESCE(COUNT(ub.branchScore), 0) AS numberOfUsers
        FROM score_branches sb
        LEFT JOIN user_branch ub ON sb.branchScore = ub.branchScore
        GROUP BY sb.branchScore
        ORDER BY sb.branchScore DESC;
        """, nativeQuery = true)
    List<UserInBranch> getNumberOfUsersInBranchAvg();

    @Query(value = """
        WITH all_branch_scores AS (
            SELECT '9.0'  AS branchScore UNION ALL
            SELECT '8.5' UNION ALL
            SELECT '8.0' UNION ALL
            SELECT '7.5' UNION ALL
            SELECT '7.0' UNION ALL
            SELECT '6.5' UNION ALL
            SELECT '6.0' UNION ALL
            SELECT '5.5' UNION ALL
            SELECT '5.0' UNION ALL
            SELECT '4.5' UNION ALL
            SELECT '4.0' UNION ALL
            SELECT '<4.0'
        ),
        best_score_per_user AS (
            SELECT 
                created_by,
                MAX(total_point) AS best_total_point
            FROM exam_attempt
            GROUP BY created_by
        ),
        branch_count AS (
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
            a.branchScore,
            COALESCE(b.numberOfUsers, 0) AS numberOfUsers
        FROM all_branch_scores a
        LEFT JOIN branch_count b ON a.branchScore = b.branchScore
        ORDER BY 
            CASE a.branchScore
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
