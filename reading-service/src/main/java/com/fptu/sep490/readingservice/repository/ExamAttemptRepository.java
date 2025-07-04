package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ExamAttempt;
import com.fptu.sep490.readingservice.model.ReadingExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    Page<ExamAttempt> findAll(Specification<ExamAttempt> spec, Pageable pageable);

}
