package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    Page<ExamAttempt> findAll(Specification<ExamAttempt> spec, Pageable pageable);

    @Query("SELECT e FROM ExamAttempt e WHERE e.createdBy = :userId AND e.totalPoint IS NOT NULL")
    List<ExamAttempt> findAllByUserId(String userId);
}
