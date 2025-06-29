package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

}
